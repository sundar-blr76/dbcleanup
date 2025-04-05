package com.dbcleanup.repository;

import com.dbcleanup.config.CleanupProperties;
import com.dbcleanup.exception.CleanupException;
import com.dbcleanup.model.CleanupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class TaskLogRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskLogRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final CleanupProperties.TaskLoggingConfig config;
    private final RowMapper<CleanupTask> taskRowMapper;

    public TaskLogRepository(JdbcTemplate jdbcTemplate, CleanupProperties.TaskLoggingConfig config) {
        super();
        this.jdbcTemplate = jdbcTemplate;
        this.config = config;
        this.taskRowMapper = new CleanupTaskRowMapper();
        ensureTaskLogTableExists();
    }

    public String logTaskStart(String taskType, String initiator, List<String> entityNames, boolean dryRun) {
        if (!config.isEnabled()) {
            return UUID.randomUUID().toString();
        }

        String taskId = UUID.randomUUID().toString();
        String sql = buildInsertTaskLogQuery();

        try {
            jdbcTemplate.update(sql,
                    taskId,
                    taskType,
                    initiator,
                    String.join(",", entityNames),
                    LocalDateTime.now(),
                    "STARTED",
                    dryRun,
                    null, // candidatesCount
                    null, // deletedCount
                    null  // errorMessage
            );

            LOGGER.info("Logged task start: taskId={}, type={}, initiator={}",
                    taskId, taskType, initiator);

            return taskId;

        } catch (DataAccessException e) {
            LOGGER.error("Error logging task start: {}", e.getMessage(), e);
            return taskId;
        }
    }

    public void logTaskCompletion(String taskId, int candidatesCount, int deletedCount) {
        if (!config.isEnabled() || taskId == null) {
            return;
        }

        String sql = "UPDATE " + getTaskLogTableName() +
                " SET status = ?, completed_at = ?, " +
                "candidates_count = ?, deleted_count = ? " +
                "WHERE task_id = ?";

        try {
            jdbcTemplate.update(sql,
                    "COMPLETED",
                    LocalDateTime.now(),
                    candidatesCount,
                    deletedCount,
                    taskId
            );

            LOGGER.info("Logged task completion: taskId={}, candidates={}, deleted={}",
                    taskId, candidatesCount, deletedCount);

        } catch (DataAccessException e) {
            LOGGER.error("Error logging task completion: {}", e.getMessage(), e);
        }
    }

    public void logTaskError(String taskId, String errorMessage) {
        if (!config.isEnabled() || taskId == null) {
            return;
        }

        String sql = "UPDATE " + getTaskLogTableName() +
                " SET status = ?, completed_at = ?, error_message = ? " +
                "WHERE task_id = ?";

        try {
            jdbcTemplate.update(sql,
                    "FAILED",
                    LocalDateTime.now(),
                    errorMessage,
                    taskId
            );

            LOGGER.info("Logged task error: taskId={}", taskId);

        } catch (DataAccessException e) {
            LOGGER.error("Error logging task error: {}", e.getMessage(), e);
        }
    }

    public List<CleanupTask> getRecentTasks(int limit) {
        String sql = "SELECT * FROM " + getTaskLogTableName() +
                " ORDER BY started_at DESC LIMIT ?";

        try {
            return jdbcTemplate.query(sql, taskRowMapper, limit);
        } catch (DataAccessException e) {
            String errorMsg = "Error retrieving recent tasks: " + e.getMessage();
            LOGGER.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    public CleanupTask getTask(String taskId) {
        String sql = "SELECT * FROM " + getTaskLogTableName() +
                " WHERE task_id = ?";

        try {
            return jdbcTemplate.queryForObject(sql, taskRowMapper, taskId);
        } catch (DataAccessException e) {
            String errorMsg = "Error retrieving task with id " + taskId + ": " + e.getMessage();
            LOGGER.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    private void ensureTaskLogTableExists() {
        // This would be better handled by schema.sql or Flyway/Liquibase in a real app
        LOGGER.info("Task log table should be created by schema.sql or database migration");
    }

    private String getTaskLogTableName() {
        if (config.getSchema() != null && !config.getSchema().isEmpty()) {
            return config.getSchema() + "." + config.getTable();
        }
        return config.getTable();
    }

    private String buildInsertTaskLogQuery() {
        return "INSERT INTO " + getTaskLogTableName() +
                " (task_id, task_type, initiator, entities, started_at, status, " +
                "dry_run, candidates_count, deleted_count, error_message) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private static class CleanupTaskRowMapper implements RowMapper<CleanupTask> {
        CleanupTaskRowMapper() {
            super();
        }

        @Override
        public CleanupTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CleanupTask(
                String.valueOf(rs.getLong("id")),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time") != null ? rs.getTimestamp("end_time").toLocalDateTime() : null,
                CleanupTask.Status.valueOf(rs.getString("status")),
                rs.getString("details")
            );
        }
    }
}
