package com.dbcleanup.config;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class CleanupConfig {    
    private List<EntityConfig> entities;
    private DatabaseConfig database;

    public CleanupConfig() {
        super();
    }

    public static CleanupConfig load(String configFile) {
        try (InputStream inputStream = CleanupConfig.class.getClassLoader().getResourceAsStream(configFile)) {
            Yaml yaml = new Yaml();
            return yaml.loadAs(inputStream, CleanupConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration file: " + configFile, e);
        }
    }

    public List<EntityConfig> getEntities() {
        return entities;
    }

    public DatabaseConfig getDatabase() {
        return database;
    }

    public static class EntityConfig {
        private String name;
        private List<Criterion> criteria;
        private int maxCleanupPercentage;
        private int maxRuntimeMinutes;
        private List<Relationship> relationships;
        private BackupConfig backup;

        public EntityConfig() {
            super();
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Criterion> getCriteria() { return criteria; }
        public void setCriteria(List<Criterion> criteria) { this.criteria = criteria; }
        public int getMaxCleanupPercentage() { return maxCleanupPercentage; }
        public void setMaxCleanupPercentage(int maxCleanupPercentage) { this.maxCleanupPercentage = maxCleanupPercentage; }
        public int getMaxRuntimeMinutes() { return maxRuntimeMinutes; }
        public void setMaxRuntimeMinutes(int maxRuntimeMinutes) { this.maxRuntimeMinutes = maxRuntimeMinutes; }
        public List<Relationship> getRelationships() { return relationships; }
        public void setRelationships(List<Relationship> relationships) { this.relationships = relationships; }
        public BackupConfig getBackup() { return backup; }
        public void setBackup(BackupConfig backup) { this.backup = backup; }
    }

    public static class Criterion {
        private String whereClause;
        private List<Parameter> parameters;

        public Criterion() {
            super();
        }

        // Getters and setters
        public String getWhereClause() { return whereClause; }
        public void setWhereClause(String whereClause) { this.whereClause = whereClause; }
        public List<Parameter> getParameters() { return parameters; }
        public void setParameters(List<Parameter> parameters) { this.parameters = parameters; }
    }

    public static class Parameter {
        private String name;
        private String value;
        private String type;

        public Parameter() {
            super();
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class Relationship {
        private String type;
        private String entity;
        private String foreignKey;

        public Relationship() {
            super();
        }

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getEntity() { return entity; }
        public void setEntity(String entity) { this.entity = entity; }
        public String getForeignKey() { return foreignKey; }
        public void setForeignKey(String foreignKey) { this.foreignKey = foreignKey; }
    }

    public static class BackupConfig {
        private boolean enabled;
        private int retentionDays;

        public BackupConfig() {
            super();
        }

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }

    public static class DatabaseConfig {
        private String url;
        private String username;
        private String password;
        private int maxPoolSize;
        private int minIdle;
        private int connectionTimeout;
        private int idleTimeout;

        public DatabaseConfig() {
            super();
        }

        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getMinIdle() { return minIdle; }
        public void setMinIdle(int minIdle) { this.minIdle = minIdle; }
        public int getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        public int getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }
    }
} 