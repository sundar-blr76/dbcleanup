package org.example.dbcleanup.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CleanupResult {
    private String taskId;
    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;
    private final Map<String, List<String>> candidateIds = new HashMap<>();
    private final Map<String, Integer> deletedCounts = new HashMap<>();
    private final Map<String, Integer> backedUpCounts = new HashMap<>();
    private final Map<String, String> errors = new HashMap<>();

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void addCandidates(String entityName, List<String> entityCandidateIds) {
        candidateIds.put(entityName, new ArrayList<>(entityCandidateIds));
    }

    public List<String> getCandidateIds(String entityName) {
        return candidateIds.getOrDefault(entityName, new ArrayList<>());
    }

    public Map<String, List<String>> getAllCandidateIds() {
        return candidateIds;
    }

    public void setDeletedCount(String entityName, int count) {
        deletedCounts.put(entityName, count);
    }

    public int getDeletedCount(String entityName) {
        return deletedCounts.getOrDefault(entityName, 0);
    }

    public void setBackedUpCount(String entityName, int count) {
        backedUpCounts.put(entityName, count);
    }

    public int getBackedUpCount(String entityName) {
        return backedUpCounts.getOrDefault(entityName, 0);
    }

    public void setError(String entityName, String error) {
        errors.put(entityName, error);
    }

    public String getError(String entityName) {
        return errors.get(entityName);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public int getTotalDeletedCount() {
        return deletedCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalBackedUpCount() {
        return backedUpCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalCandidateCount() {
        return candidateIds.values().stream().mapToInt(List::size).sum();
    }

    public void complete() {
        this.endTime = LocalDateTime.now();
    }

    public void merge(PartialCleanupResult partialResult) {
        // Merge partial result into this result
        for (String entity : partialResult.getCandidateEntities()) {
            List<String> existingCandidates = candidateIds.getOrDefault(entity, new ArrayList<>());
            existingCandidates.addAll(partialResult.getCandidateIds(entity));
            candidateIds.put(entity, existingCandidates);
        }

        for (String entity : partialResult.getDeletedEntities()) {
            int existingCount = deletedCounts.getOrDefault(entity, 0);
            deletedCounts.put(entity, existingCount + partialResult.getDeletedCount(entity));
        }

        for (String entity : partialResult.getBackedUpEntities()) {
            int existingCount = backedUpCounts.getOrDefault(entity, 0);
            backedUpCounts.put(entity, existingCount + partialResult.getBackedUpCount(entity));
        }

        for (String entity : partialResult.getErrorEntities()) {
            errors.put(entity, partialResult.getError(entity));
        }
    }
}