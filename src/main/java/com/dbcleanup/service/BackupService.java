package com.dbcleanup.service;

import com.dbcleanup.config.CleanupProperties.EntityConfig;
import com.dbcleanup.exception.CleanupException;
import com.dbcleanup.repository.TaskLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BackupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final TaskLogRepository taskLogRepository;

    public BackupService(JdbcTemplate jdbcTemplate, TaskLogRepository taskLogRepository) {
        super();
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.taskLogRepository = taskLogRepository;
    }

    /**
     * Backup candidates directly using an INSERT..SELECT statement
     */
    @Transactional
    public int backupCandidatesDirect(EntityConfig entityConfig, String taskId, List<String> candidateIds) {
        if (!entityConfig.getBackup().isEnabled()) {
            return 0;
        }

        String backupTable = entityConfig.getBackup().getTable();
        if (backupTable == null || backupTable.isEmpty()) {
            LOGGER.warn("No backup table specified for entity {}", entityConfig.getName());
            return 0;
        }

        try {
            int backedUp;

            if (candidateIds.isEmpty()) {
                LOGGER.info("No candidates to backup for entity {}", entityConfig.getName());
                return 0;
            }

            // For large number of candidates, batch the backup process
            if (candidateIds.size() > 1000) {
                backedUp = backupInBatches(entityConfig, taskId, candidateIds);
            } else {
                String backupQuery = buildBackupQuery(entityConfig, taskId, candidateIds);
                backedUp = jdbcTemplate.update(backupQuery);
            }

            LOGGER.info("Backed up {} records for entity {}", backedUp, entityConfig.getName());
            return backedUp;
        } catch (Exception e) {
            String errorMsg = "Error backing up candidates for " + entityConfig.getName() + ": " + e.getMessage();
            LOGGER.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Backup candidates in batches for large datasets
     */
    private int backupInBatches(EntityConfig entityConfig, String taskId, List<String> candidateIds) {
        int batchSize = 1000;
        int totalBackedUp = 0;

        for (int i = 0; i < candidateIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, candidateIds.size());
            List<String> batchIds = candidateIds.subList(i, endIndex);

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("taskId", taskId);
            params.addValue("candidateIds", batchIds);

            String backupQuery = buildParameterizedBackupQuery(entityConfig);
            int backedUp = namedParameterJdbcTemplate.update(backupQuery, params);
            totalBackedUp += backedUp;

            LOGGER.debug("Backed up batch {} of {} for entity {}: {} records",
                    (i / batchSize) + 1, (candidateIds.size() / batchSize) + 1,
                    entityConfig.getName(), backedUp);
        }

        return totalBackedUp;
    }

    /**
     * Retrieve backup metadata for specific entity and criteria
     */
    public List<Map<String, Object>> getBackupMetadata(String entityName, String taskId, int limit) {
        String sql = "SELECT backup_id, entity_id, backup_time, reinstated, reinstated_time, reinstated_by " +
                "FROM " + getBackupTableName(entityName) + " " +
                "WHERE task_id = ? " +
                "ORDER BY backup_time DESC " +
                "LIMIT ?";

        try {
            return jdbcTemplate.queryForList(sql, taskId, limit);
        } catch (Exception e) {
            String errorMsg = "Error retrieving backup metadata for " + entityName + ": " + e.getMessage();
            LOGGER.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Retrieve backup data for a specific backup ID
     */
    public Map<String, Object> getBackupData(String entityName, String backupId) {
        String sql = "SELECT backup_data " +
                "FROM " + getBackupTableName(entityName) + " " +
                "WHERE backup_id = ?";

        try {
            return jdbcTemplate.queryForMap(sql, backupId);
        } catch (Exception e) {
            String errorMsg = "Error retrieving backup data for ID " + backupId + ": " + e.getMessage();
            LOGGER.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Reinstate specific backup records
     */
    @Transactional
    public int reinstateBackups(String entityName, List<String> backupIds, String initiator) {
        if (backupIds == null || backupIds.isEmpty()) {
            return 0;
        }

        String taskId = taskLogRepository.logTaskStart(
                "REINSTATE", initiator, List.of(entityName), false);

        try {
            String backupTable = getBackupTableName(entityName);
            String originalTable = getOriginalTableName(entityName, backupTable);

            // First, check that all backup IDs exist and are not already reinstated
            String checkSql = "SELECT COUNT(*) FROM " + backupTable +
                    " WHERE backup_id IN (:backupIds) AND reinstated = FALSE";

            MapSqlParameterSource checkParams = new MapSqlParameterSource();
            checkParams.addValue("backupIds", backupIds);

            int eligibleCount = namedParameterJdbcTemplate.queryForObject(checkSql, checkParams, Integer.class);

            if (eligibleCount != backupIds.size()) {
                throw new CleanupException("Some backup IDs do not exist or are already reinstated");
            }

            // Next, insert back into original table
            String reinstateQuery = "INSERT INTO " + originalTable + " " +
                    "SELECT (backup_data->>'id')::uuid as id, " +
                    "/* Map other fields from backup_data JSON */ " +
                    "FROM " + backupTable + " " +
                    "WHERE backup_id IN (:backupIds) AND reinstated = FALSE";

            int reinstated = namedParameterJdbcTemplate.update(reinstateQuery, checkParams);

            // Finally, mark as reinstated
            String markQuery = "UPDATE " + backupTable + " " +
                    "SET reinstated = TRUE, " +
                    "reinstated_time = :now, " +
                    "reinstated_by = :initiator " +
                    "WHERE backup_id IN (:backupIds)";

            MapSqlParameterSource markParams = new MapSqlParameterSource();
            markParams.addValue("backupIds", backupIds);
            markParams.addValue("now", LocalDateTime.now());
            markParams.addValue("initiator", initiator);

            namedParameterJdbcTemplate.update(markQuery, markParams);

            taskLogRepository.logTaskCompletion(taskId, backupIds.size(), reinstated);

            LOGGER.info("Reinstated {} records for entity {}", reinstated, entityName);
            return reinstated;

        } catch (Exception e) {
            String errorMsg = "Error reinstating backups: " + e.getMessage();
            LOGGER.error(errorMsg, e);
            taskLogRepository.logTaskError(taskId, errorMsg);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Purge old backup records based on retention policy
     */
    @Transactional
    public int purgeOldBackups(String entityName, int retentionDays) {
        String backupTable = getBackupTableName(entityName);

        String purgeQuery = "DELETE FROM " + backupTable + " " +
                "WHERE backup_time < CURRENT_DATE - INTERVAL '" + retentionDays + " days'";

        try {
            int purged = jdbcTemplate.update(purgeQuery);
            LOGGER.info("Purged {} old backup records for entity {}", purged, entityName);
            return purged;
        } catch (Exception e) {
            String errorMsg = "Error purging old backups for " + entityName + ": " + e.getMessage();
            LOGGER.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    private String buildBackupQuery(EntityConfig entityConfig, String taskId, List<String> candidateIds) {
        String backupTable = entityConfig.getBackup().getTable();
        String schema = entityConfig.getBackup().getSchema();
        String fullTableName = schema != null && !schema.isEmpty() ?
                schema + "." + backupTable : backupTable;

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(fullTableName).append(" (");

        // Define columns for the backup table
        sb.append("backup_id, task_id, entity_id, backup_time, reinstated, original_table, backup_data");

        sb.append(") SELECT ");

        // Generate a UUID for each record
        sb.append("uuid_generate_v4(), "); // backup_id
        // or alternative approach if uuid_generate_v4() isn't available:
        // sb.append("'").append(UUID.randomUUID().toString()).append("', ");

        sb.append("'").append(taskId).append("', "); // task_id
        sb.append("id, "); // entity_id
        sb.append("CURRENT_TIMESTAMP, "); // backup_time
        sb.append("FALSE, "); // reinstated
        sb.append("'").append(entityConfig.getTable()).append("', "); // original_table

        // Generate JSON backup data containing all columns
        sb.append("to_jsonb(e) "); // backup_data (PostgreSQL syntax)

        sb.append("FROM ").append(entityConfig.getTable()).append(" e ");
        sb.append("WHERE e.id IN (");

        // Add candidate IDs
        for (int i = 0; i < candidateIds.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("'").append(candidateIds.get(i)).append("'");
        }

        sb.append(")");

        return sb.toString();
    }

    private String buildParameterizedBackupQuery(EntityConfig entityConfig) {
        String backupTable = entityConfig.getBackup().getTable();
        String schema = entityConfig.getBackup().getSchema();
        String fullTableName = schema != null && !schema.isEmpty() ?
                schema + "." + backupTable : backupTable;

        String sb = "INSERT INTO " + fullTableName + " (" +

                // Define columns for the backup table
                "backup_id, task_id, entity_id, backup_time, reinstated, original_table, backup_data" +
                ") SELECT " +

                // Values for each column
                "uuid_generate_v4(), " + // backup_id
                ":taskId, " + // task_id
                "e.id, " + // entity_id
                "CURRENT_TIMESTAMP, " + // backup_time
                "FALSE, " + // reinstated
                "'" + entityConfig.getTable() + "', " + // original_table
                "to_jsonb(e) " + // backup_data

                "FROM " + entityConfig.getTable() + " e " +
                "WHERE e.id IN (:candidateIds)";

        return sb;
    }

    private String getBackupTableName(String entityName) {
        return entityName.toLowerCase() + "_backup";
    }

    private String getOriginalTableName(String entityName, String backupTable) {
        // This is a simplistic approach - ideally, you'd store the original table name in the backup
        return backupTable.replace("_backup", "");
    }
}
