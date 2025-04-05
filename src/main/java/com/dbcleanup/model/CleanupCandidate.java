package com.dbcleanup.model;

import java.util.HashMap;
import java.util.Map;

public class CleanupCandidate {
    private String entityName;
    private String tableName;
    private String id;
    private Map<String, Object> attributes = new HashMap<>();
    private boolean selected;

    public CleanupCandidate() {
        super();
    }

    // Getters and setters
    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }
}