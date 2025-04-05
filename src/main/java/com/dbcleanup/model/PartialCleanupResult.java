package com.dbcleanup.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;

public class PartialCleanupResult {
    public PartialCleanupResult() {
        super();
    }
    
    private final Map<String, List<String>> candidateIds = new HashMap<>();
    private final Map<String, Integer> deletedCounts = new HashMap<>();
    private final Map<String, Integer> backedUpCounts = new HashMap<>();
    private final Map<String, String> errors = new HashMap<>();

    public void addCandidates(String entityName, List<String> entityCandidateIds) {
        candidateIds.put(entityName, new ArrayList<>(entityCandidateIds));
    }

    public Set<String> getCandidateEntities() {
        return candidateIds.keySet();
    }

    public Set<String> getDeletedEntities() {
        return deletedCounts.keySet();
    }

    public Set<String> getBackedUpEntities() {
        return backedUpCounts.keySet();
    }

    public Set<String> getErrorEntities() {
        return errors.keySet();
    }

    public List<String> getCandidateIds(String entityName) {
        return candidateIds.getOrDefault(entityName, new ArrayList<>());
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
}
