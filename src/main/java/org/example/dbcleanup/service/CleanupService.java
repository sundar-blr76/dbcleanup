package org.example.dbcleanup.service;

import org.example.dbcleanup.config.CleanupProperties;
import org.example.dbcleanup.config.CleanupProperties.EntityConfig;
import org.example.dbcleanup.exception.CleanupException;
import org.example.dbcleanup.model.CleanupResult;
import org.example.dbcleanup.model.CleanupTask;
import org.example.dbcleanup.repository.CleanupRepository;
import org.example.dbcleanup.repository.TaskLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CleanupService {
    private static final Logger logger = LoggerFactory.getLogger(CleanupService.class);

    private final CleanupProperties properties;
    private final CleanupRepository cleanupRepository;
    private final TaskLogRepository taskLogRepository;
    private final DistributedCleanupService distributedCleanupService;

    public CleanupService(
            CleanupProperties properties,
            CleanupRepository cleanupRepository,
            TaskLogRepository taskLogRepository,
            DistributedCleanupService distributedCleanupService) {
        this.properties = properties;
        this.cleanupRepository = cleanupRepository;
        this.taskLogRepository = taskLogRepository;
        this.distributedCleanupService = distributedCleanupService;
    }

    /**
     * Perform analysis only (dry run) without actual deletions
     */
    @Transactional(readOnly = true)
    public CleanupResult analyzeCleanupCandidates(String initiator) {
        logger.info("Starting cleanup analysis (dry run)");

        List<String> entityNames = properties.getEntities().stream()
                .map(EntityConfig::getName)
                .collect(Collectors.toList());

        String taskId = taskLogRepository.logTaskStart(                "ANALYSIS", initiator, entityNames, true);

        CleanupResult result = new CleanupResult();
        result.setTaskId(taskId);

        try {
            for (EntityConfig entityConfig : properties.getEntities()) {
                List<String> candidateIds = cleanupRepository.findCandidateIds(entityConfig);
                result.addCandidates(entityConfig.getName(), candidateIds);

                logger.info("Found {} cleanup candidates for entity {}",
                        candidateIds.size(), entityConfig.getName());
            }

            result.complete();
            taskLogRepository.logTaskCompletion(
                    taskId, result.getTotalCandidateCount(), 0);

            return result;

        } catch (Exception e) {
            String errorMsg = "Error during cleanup analysis: " + e.getMessage();
            logger.error(errorMsg, e);
            taskLogRepository.logTaskError(taskId, errorMsg);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Execute actual cleanup with backup
     */
    @Transactional
    public CleanupResult executeCleanup(String initiator, boolean dryRun) {
        logger.info("Starting cleanup execution, dryRun={}", dryRun);

        // If distributed mode is enabled, delegate to distributed service
        if (properties.getDistribution() != null &&
                properties.getDistribution().isEnabled() && !dryRun) {
            return distributedCleanupService.executeDistributedCleanup(initiator, dryRun);
        }

        List<String> entityNames = properties.getEntities().stream()
                .map(EntityConfig::getName)
                .collect(Collectors.toList());

        String taskId = taskLogRepository.logTaskStart(
                "CLEANUP", initiator, entityNames, dryRun);

        CleanupResult result = new CleanupResult();
        result.setTaskId(taskId);

        try {
            for (EntityConfig entityConfig : properties.getEntities()) {
                // Find candidate IDs
                List<String> candidateIds = cleanupRepository.findCandidateIds(entityConfig);
                result.addCandidates(entityConfig.getName(), candidateIds);

                if (candidateIds.isEmpty()) {
                    logger.info("No cleanup candidates for entity {}", entityConfig.getName());
                    continue;
                }

                // Backup if configured and not dry run
                if (entityConfig.getBackup() != null &&
                        entityConfig.getBackup().isEnabled() && !dryRun) {
                    int backedUp = cleanupRepository.backupCandidatesDirect(entityConfig, taskId);
                    result.setBackedUpCount(entityConfig.getName(), backedUp);
                }

                // Delete if not dry run
                if (!dryRun) {
                    int deleted = cleanupRepository.deleteCandidatesDirect(entityConfig);
                    result.setDeletedCount(entityConfig.getName(), deleted);
                }
            }

            result.complete();
            taskLogRepository.logTaskCompletion(
                    taskId, result.getTotalCandidateCount(), result.getTotalDeletedCount());

            return result;

        } catch (Exception e) {
            String errorMsg = "Error during cleanup execution: " + e.getMessage();
            logger.error(errorMsg, e);
            taskLogRepository.logTaskError(taskId, errorMsg);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Reinstate previously backed up records
     */
    @Transactional
    public int reinstateBackups(String entityName, List<String> backupIds, String initiator) {
        logger.info("Reinstating {} backup records for entity {}", backupIds.size(), entityName);

        if (backupIds == null || backupIds.isEmpty()) {
            return 0;
        }

        String taskId = taskLogRepository.logTaskStart(
                "REINSTATE", initiator, List.of(entityName), false);

        try {
            int reinstated = cleanupRepository.reinstateBackups(entityName, backupIds);

            taskLogRepository.logTaskCompletion(taskId, backupIds.size(), reinstated);

            return reinstated;

        } catch (Exception e) {
            String errorMsg = "Error reinstating backups: " + e.getMessage();
            logger.error(errorMsg, e);
            taskLogRepository.logTaskError(taskId, errorMsg);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Get historical cleanup tasks
     */
    public List<CleanupTask> getRecentTasks(int limit) {
        return taskLogRepository.getRecentTasks(limit);
    }

    /**
     * Get a specific task by ID
     */
    public CleanupTask getTask(String taskId) {
        return taskLogRepository.getTask(taskId);
    }
}