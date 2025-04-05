package com.dbcleanup.service;

import com.dbcleanup.config.CleanupProperties;
import com.dbcleanup.config.CleanupProperties.EntityConfig;
import com.dbcleanup.exception.CleanupException;
import com.dbcleanup.model.CleanupResult;
import com.dbcleanup.model.CleanupTask;
import com.dbcleanup.repository.CleanupRepository;
import com.dbcleanup.repository.TaskLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CleanupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupService.class);

    private final CleanupProperties properties;
    private final CleanupRepository cleanupRepository;
    private final TaskLogRepository taskLogRepository;
    private final DistributedCleanupService distributedCleanupService;

    public CleanupService(
            CleanupProperties properties,
            CleanupRepository cleanupRepository,
            TaskLogRepository taskLogRepository,
            DistributedCleanupService distributedCleanupService) {
        super();
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
        LOGGER.info("Starting cleanup analysis (dry run)");

        List<String> entityNames = properties.getEntities().stream()
                .map(EntityConfig::getName)
                .collect(Collectors.toList());

        String taskId = taskLogRepository.logTaskStart("ANALYSIS", initiator, entityNames, true);

        CleanupResult result = new CleanupResult();
        result.setTaskId(taskId);

        try {
            for (EntityConfig entityConfig : properties.getEntities()) {
                List<String> candidateIds = cleanupRepository.findCandidateIds(entityConfig);
                result.addCandidates(entityConfig.getName(), candidateIds);

                LOGGER.info("Found {} cleanup candidates for entity {}",
                        candidateIds.size(), entityConfig.getName());
            }

            result.complete();
            taskLogRepository.logTaskCompletion(
                    taskId, result.getTotalCandidateCount(), 0);

            return result;

        } catch (Exception e) {
            String errorMsg = "Error during cleanup analysis: " + e.getMessage();
            LOGGER.error(errorMsg, e);
            taskLogRepository.logTaskError(taskId, errorMsg);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Execute actual cleanup with backup
     */
    @Transactional
    public CleanupResult executeCleanup(String initiator, boolean dryRun) {
        LOGGER.info("Starting cleanup execution. Initiator: {}, Dry run: {}", initiator, dryRun);
        
        List<String> entityNames = properties.getEntities().stream()
                .map(EntityConfig::getName)
                .collect(Collectors.toList());
        
        String taskId = taskLogRepository.logTaskStart("CLEANUP", initiator, entityNames, dryRun);
        
        CleanupResult result = new CleanupResult();
        result.setTaskId(taskId);
        
        try {
            if (properties.getDistribution() != null 
                    && properties.getDistribution().getWorkerCount() > 1) {
                return distributedCleanupService.executeDistributedCleanup(initiator, dryRun);
            }
            
            return executeLocalCleanup(taskId, initiator, dryRun);
        } catch (Exception e) {
            LOGGER.error("Error during cleanup execution", e);
            taskLogRepository.logTaskError(taskId, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute cleanup locally (non-distributed)
     */
    private CleanupResult executeLocalCleanup(String taskId, String initiator, boolean dryRun) {
        LOGGER.info("Executing local cleanup. Initiator: {}, Dry run: {}", initiator, dryRun);
        
        CleanupResult result = new CleanupResult();
        result.setTaskId(taskId);
        
        try {
            for (EntityConfig entityConfig : properties.getEntities()) {
                List<String> candidateIds = cleanupRepository.findCandidateIds(entityConfig);
                result.addCandidates(entityConfig.getName(), candidateIds);
                
                if (dryRun) {
                    LOGGER.info("Found {} cleanup candidates for entity {}",
                            candidateIds.size(), entityConfig.getName());
                } else {
                    // First backup if enabled
                    if (entityConfig.getBackup() != null && entityConfig.getBackup().isEnabled()) {
                        int backedUp = cleanupRepository.backupCandidatesDirect(entityConfig, taskId);
                        result.setBackedUpCount(entityConfig.getName(), backedUp);
                    }
                    
                    // Then delete
                    int deleted = cleanupRepository.deleteCandidatesDirect(entityConfig);
                    result.setDeletedCount(entityConfig.getName(), deleted);
                    LOGGER.info("Deleted {} records for entity {}",
                            deleted, entityConfig.getName());
                }
            }
            
            result.complete();
            taskLogRepository.logTaskCompletion(taskId, result.getTotalCandidateCount(), result.getTotalDeletedCount());
            return result;
            
        } catch (Exception e) {
            String errorMsg = "Error during local cleanup execution: " + e.getMessage();
            LOGGER.error(errorMsg, e);
            taskLogRepository.logTaskError(taskId, errorMsg);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Reinstate previously backed up records
     */
    @Transactional
    public int reinstateBackups(String entityName, List<String> backupIds, String initiator) {
        LOGGER.info("Reinstating {} backup records for entity {}", backupIds.size(), entityName);

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
            LOGGER.error(errorMsg, e);
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
