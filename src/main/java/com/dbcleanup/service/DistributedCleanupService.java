package com.dbcleanup.service;

import com.dbcleanup.config.CleanupProperties;
import com.dbcleanup.config.CleanupProperties.EntityConfig;
import com.dbcleanup.model.CleanupResult;
import com.dbcleanup.model.PartialCleanupResult;
import com.dbcleanup.repository.CleanupRepository;
import com.dbcleanup.repository.TaskLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DistributedCleanupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedCleanupService.class);

    private final CleanupProperties properties;
    private final CleanupRepository cleanupRepository;
    private final TaskLogRepository taskLogRepository;

    public DistributedCleanupService(
            CleanupProperties properties,
            CleanupRepository cleanupRepository,
            TaskLogRepository taskLogRepository) {
        super();
        this.properties = properties;
        this.cleanupRepository = cleanupRepository;
        this.taskLogRepository = taskLogRepository;
    }

    public CleanupResult executeDistributedCleanup(String initiator, boolean dryRun) {
        LOGGER.info("Starting distributed cleanup, dryRun={}", dryRun);

        List<String> entityNames = properties.getEntities().stream()
                .map(EntityConfig::getName)
                .collect(Collectors.toList());

        String taskId = taskLogRepository.logTaskStart(
                "DISTRIBUTED_CLEANUP", initiator, entityNames, dryRun);

        CleanupResult result = new CleanupResult();
        result.setTaskId(taskId);

        List<CompletableFuture<PartialCleanupResult>> futures = properties.getEntities().stream()
                .map(entityConfig -> processEntityAsync(entityConfig, taskId, dryRun))
                .collect(Collectors.toList());

        // Wait for all tasks to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Merge results
            for (CompletableFuture<PartialCleanupResult> future : futures) {
                result.merge(future.get());
            }

            result.complete();
            taskLogRepository.logTaskCompletion(
                    taskId, result.getTotalCandidateCount(), result.getTotalDeletedCount());

            return result;

        } catch (Exception e) {
            String errorMsg = "Error during distributed cleanup: " + e.getMessage();
            LOGGER.error(errorMsg, e);
            taskLogRepository.logTaskError(taskId, errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Async("cleanupTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<PartialCleanupResult> processEntityAsync(
            EntityConfig entityConfig, String taskId, boolean dryRun) {

        LOGGER.info("Processing entity {} asynchronously", entityConfig.getName());

        PartialCleanupResult partialResult = new PartialCleanupResult();

        try {
            // Find candidate IDs
            List<String> candidateIds = cleanupRepository.findCandidateIds(entityConfig);
            partialResult.addCandidates(entityConfig.getName(), candidateIds);

            if (candidateIds.isEmpty()) {
                LOGGER.info("No cleanup candidates for entity {}", entityConfig.getName());
                return CompletableFuture.completedFuture(partialResult);
            }

            // Backup if configured and not dry run
            if (entityConfig.getBackup() != null &&
                    entityConfig.getBackup().isEnabled() && !dryRun) {
                int backedUp = cleanupRepository.backupCandidatesDirect(entityConfig, taskId);
                partialResult.setBackedUpCount(entityConfig.getName(), backedUp);
            }

            // Delete if not dry run
            if (!dryRun) {
                int deleted = cleanupRepository.deleteCandidatesDirect(entityConfig);
                partialResult.setDeletedCount(entityConfig.getName(), deleted);
            }

            return CompletableFuture.completedFuture(partialResult);

        } catch (Exception e) {
            String errorMsg = "Error processing entity " + entityConfig.getName() + ": " + e.getMessage();
            LOGGER.error(errorMsg, e);
            partialResult.setError(entityConfig.getName(), errorMsg);
            return CompletableFuture.completedFuture(partialResult);
        }
    }
}
