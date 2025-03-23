package org.example.dbcleanup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "cleanup")
@Component
public class CleanupProperties {
    private List<EntityConfig> entities;
    private DistributionConfig distribution;
    private TaskLoggingConfig taskLogging = new TaskLoggingConfig();
    private SchedulerConfig scheduler = new SchedulerConfig();

    // Getters and setters
    public List<EntityConfig> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityConfig> entities) {
        this.entities = entities;
    }

    public DistributionConfig getDistribution() {
        return distribution;
    }

    public void setDistribution(DistributionConfig distribution) {
        this.distribution = distribution;
    }

    public TaskLoggingConfig getTaskLogging() {
        return taskLogging;
    }

    public void setTaskLogging(TaskLoggingConfig taskLogging) {
        this.taskLogging = taskLogging;
    }

    public SchedulerConfig getScheduler() {
        return scheduler;
    }

    public void setScheduler(SchedulerConfig scheduler) {
        this.scheduler = scheduler;
    }

    public enum JoinType {
        INNER, LEFT, RIGHT
    }

    public static class EntityConfig {
        private String name;
        private String table;
        private List<CriteriaConfig> criteria;
        private List<RelatedEntityConfig> related;
        private boolean transactionBoundary;
        private BackupConfig backup = new BackupConfig();
        private Map<String, Object> additionalProperties;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public List<CriteriaConfig> getCriteria() {
            return criteria;
        }

        public void setCriteria(List<CriteriaConfig> criteria) {
            this.criteria = criteria;
        }

        public List<RelatedEntityConfig> getRelated() {
            return related;
        }

        public void setRelated(List<RelatedEntityConfig> related) {
            this.related = related;
        }

        public boolean isTransactionBoundary() {
            return transactionBoundary;
        }

        public void setTransactionBoundary(boolean transactionBoundary) {
            this.transactionBoundary = transactionBoundary;
        }

        public BackupConfig getBackup() {
            return backup;
        }

        public void setBackup(BackupConfig backup) {
            this.backup = backup;
        }

        public Map<String, Object> getAdditionalProperties() {
            return additionalProperties;
        }

        public void setAdditionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }
    }

    public static class CriteriaConfig {
        private String field;
        private String condition;
        private String operator = "AND";
        private String referencedEntity;
        private String referencedField;

        // Getters and setters
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getReferencedEntity() {
            return referencedEntity;
        }

        public void setReferencedEntity(String referencedEntity) {
            this.referencedEntity = referencedEntity;
        }

        public String getReferencedField() {
            return referencedField;
        }

        public void setReferencedField(String referencedField) {
            this.referencedField = referencedField;
        }
    }

    public static class RelatedEntityConfig {
        private String entity;
        private String table;
        private String join;
        private String foreignKey;
        private JoinType joinType = JoinType.INNER;
        private boolean cascadeDelete = false;

        // Getters and setters
        public String getEntity() {
            return entity;
        }

        public void setEntity(String entity) {
            this.entity = entity;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getJoin() {
            return join;
        }

        public void setJoin(String join) {
            this.join = join;
        }

        public String getForeignKey() {
            return foreignKey;
        }

        public void setForeignKey(String foreignKey) {
            this.foreignKey = foreignKey;
        }

        public JoinType getJoinType() {
            return joinType;
        }

        public void setJoinType(JoinType joinType) {
            this.joinType = joinType;
        }

        public boolean isCascadeDelete() {
            return cascadeDelete;
        }

        public void setCascadeDelete(boolean cascadeDelete) {
            this.cascadeDelete = cascadeDelete;
        }
    }

    public static class BackupConfig {
        private boolean enabled = true;
        private String table;
        private String schema;

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }
    }

    public static class TaskLoggingConfig {
        private boolean enabled = true;
        private String table = "cleanup_task_log";
        private String schema;
        private int retentionDays = 90;

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }

    public static class DistributionConfig {
        private boolean enabled = false;
        private int workerCount = 4;
        private String partitionBy;
        private int batchSize = 1000;

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getWorkerCount() {
            return workerCount;
        }

        public void setWorkerCount(int workerCount) {
            this.workerCount = workerCount;
        }

        public String getPartitionBy() {
            return partitionBy;
        }

        public void setPartitionBy(String partitionBy) {
            this.partitionBy = partitionBy;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class SchedulerConfig {
        private boolean enabled = false;
        private String cron = "0 0 2 * * *"; // Default: 2 AM daily
        private boolean dryRun = false;

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }
    }
}