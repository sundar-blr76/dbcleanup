package com.dbcleanup.repository;

import com.dbcleanup.config.CleanupProperties;
import com.dbcleanup.config.CleanupProperties.CriteriaConfig;
import com.dbcleanup.config.CleanupProperties.EntityConfig;
import com.dbcleanup.config.CleanupProperties.RelatedEntityConfig;
import com.dbcleanup.exception.CleanupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class CleanupRepository {
    private static final Logger logger = LoggerFactory.getLogger(CleanupRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final Map<String, CleanupProperties.EntityConfig> entityConfigMap;

    public CleanupRepository(JdbcTemplate jdbcTemplate, List<EntityConfig> entityConfigs) {
        super(); // Add explicit super() call
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        // Create a map for quick lookups of entity configs by name
        this.entityConfigMap = new HashMap<>();
        if (entityConfigs != null) {
            for (EntityConfig config : entityConfigs) {
                entityConfigMap.put(config.getName(), config);
            }
        }
    }

    public CleanupRepository() {
        super();
    }

    /**
     * Find candidates by executing a query with all criteria conditions
     * For performance, this returns only IDs
     */
    public List<String> findCandidateIds(EntityConfig entityConfig) {
        String query = buildFindCandidatesQuery(entityConfig);

        try {
            List<String> ids = jdbcTemplate.queryForList(query, String.class);
            logger.info("Found {} candidates for entity {}", ids.size(), entityConfig.getName());
            return ids;
        } catch (Exception e) {
            String errorMsg = "Error finding cleanup candidates for " + entityConfig.getName() + ": " + e.getMessage();
            logger.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Backup candidates directly using an INSERT..SELECT statement
     */
    public int backupCandidatesDirect(EntityConfig entityConfig, String taskId) {
        if (!entityConfig.getBackup().isEnabled()) {
            return 0;
        }

        String backupTable = entityConfig.getBackup().getTable();
        if (backupTable == null || backupTable.isEmpty()) {
            logger.warn("No backup table specified for entity {}", entityConfig.getName());
            return 0;
        }

        String query = buildBackupQuery(entityConfig, taskId);

        try {
            int backedUp = jdbcTemplate.update(query);
            logger.info("Backed up {} records for entity {}", backedUp, entityConfig.getName());
            return backedUp;
        } catch (Exception e) {
            String errorMsg = "Error backing up candidates for " + entityConfig.getName() + ": " + e.getMessage();
            logger.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Delete candidates directly using a DELETE statement that includes the criteria
     */
    public int deleteCandidatesDirect(EntityConfig entityConfig) {
        String deleteQuery = buildDirectDeleteQuery(entityConfig);

        try {
            // First delete from related entities if cascade delete is enabled
            int relatedDeleted = 0;
            if (entityConfig.getRelated() != null) {
                for (RelatedEntityConfig relatedConfig : entityConfig.getRelated()) {
                    if (relatedConfig.isCascadeDelete()) {
                        String relatedQuery = buildRelatedDeleteQuery(entityConfig, relatedConfig);
                        int count = jdbcTemplate.update(relatedQuery);
                        relatedDeleted += count;
                        logger.info("Deleted {} related records from {}",
                                count, relatedConfig.getEntity());
                    }
                }
            }

            // Then delete from the main entity
            int deleted = jdbcTemplate.update(deleteQuery);
            logger.info("Deleted {} records from {}", deleted, entityConfig.getTable());

            return deleted;
        } catch (Exception e) {
            String errorMsg = "Error deleting candidates for " + entityConfig.getName() + ": " + e.getMessage();
            logger.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    /**
     * Reinstate previously backed up records
     */
    public int reinstateBackups(String entityName, List<String> backupIds) {
        EntityConfig entityConfig = entityConfigMap.get(entityName);
        if (entityConfig == null) {
            throw new CleanupException("Entity config not found for " + entityName);
        }

        if (!entityConfig.getBackup().isEnabled()) {
            throw new CleanupException("Backup not enabled for entity " + entityName);
        }

        String backupTable = entityConfig.getBackup().getTable();
        if (backupTable == null || backupTable.isEmpty()) {
            throw new CleanupException("No backup table specified for entity " + entityName);
        }

        // First, insert records back to the original table
        String reinstateQuery = buildReinstateQuery(entityConfig);

        // Then, mark the backup records as reinstated
        String markReinstatedQuery = buildMarkReinstatedQuery(entityConfig);

        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("backupIds", backupIds);

            // First insert back to the original table
            int reinstated = namedParameterJdbcTemplate.update(reinstateQuery, params);

            // Then mark backups as reinstated
            params.addValue("username", "system"); // TODO: Get actual username
            namedParameterJdbcTemplate.update(markReinstatedQuery, params);

            logger.info("Reinstated {} records for entity {}", reinstated, entityName);
            return reinstated;
        } catch (Exception e) {
            String errorMsg = "Error reinstating backups for " + entityName + ": " + e.getMessage();
            logger.error(errorMsg, e);
            throw new CleanupException(errorMsg, e);
        }
    }

    private String buildFindCandidatesQuery(EntityConfig entityConfig) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT e.id FROM ").append(entityConfig.getTable()).append(" e");

        // Add necessary joins for related entity criteria
        Map<String, String> tableAliases = new HashMap<>();
        tableAliases.put(entityConfig.getName(), "e");

        // Track which tables we've already joined
        List<String> joinedTables = new ArrayList<>();

        // Check for criteria that reference other entities
        if (entityConfig.getCriteria() != null) {
            for (CriteriaConfig criteria : entityConfig.getCriteria()) {
                if (criteria.getReferencedEntity() != null && !criteria.getReferencedEntity().isEmpty()) {
                    // We need to join to this entity
                    String referencedEntity = criteria.getReferencedEntity();
                    if (!joinedTables.contains(referencedEntity)) {
                        EntityConfig refConfig = entityConfigMap.get(referencedEntity);
                        if (refConfig != null) {
                            String alias = "r" + joinedTables.size();
                            tableAliases.put(referencedEntity, alias);

                            // Find the relationship
                            RelatedEntityConfig relation = findRelationshipBetween(
                                    entityConfig.getName(), referencedEntity);

                            if (relation != null) {
                                // Add the join clause
                                sb.append(" INNER JOIN ").append(refConfig.getTable())
                                        .append(" ").append(alias)
                                        .append(" ON ");

                                if (relation.getEntity().equals(referencedEntity)) {
                                    // Main entity joins to referenced entity
                                    sb.append("e.").append(relation.getJoin())
                                            .append(" = ").append(alias).append(".id");
                                } else {
                                    // Referenced entity joins to main entity
                                    sb.append("e.id")
                                            .append(" = ").append(alias).append(".")
                                            .append(relation.getJoin());
                                }

                                joinedTables.add(referencedEntity);
                            } else {
                                logger.warn("No relationship found between {} and {}",
                                        entityConfig.getName(), referencedEntity);
                            }
                        }
                    }
                }
            }
        }

        // Add WHERE clause
        sb.append(" WHERE ");
        boolean firstCriteria = true;

        if (entityConfig.getCriteria() != null) {
            for (CriteriaConfig criteria : entityConfig.getCriteria()) {
                if (!firstCriteria) {
                    sb.append(" ").append(criteria.getOperator()).append(" ");
                }
                firstCriteria = false;

                if (criteria.getReferencedEntity() != null && !criteria.getReferencedEntity().isEmpty()) {
                    // This is a criteria on a referenced entity
                    String refAlias = tableAliases.get(criteria.getReferencedEntity());
                    if (refAlias != null) {
                        sb.append(refAlias).append(".")
                                .append(criteria.getReferencedField())
                                .append(" ").append(criteria.getCondition());
                    } else {
                        sb.append("1=1"); // Dummy condition that's always true
                    }
                } else {
                    // This is a criteria on the main entity
                    sb.append("e.").append(criteria.getField())
                            .append(" ").append(criteria.getCondition());
                }
            }
        }

        if (firstCriteria) {
            // No criteria specified, add a dummy condition
            sb.append("1=1");
        }

        return sb.toString();
    }

    private String buildBackupQuery(EntityConfig entityConfig, String taskId) {
        String backupTable = entityConfig.getBackup().getTable();
        String schema = entityConfig.getBackup().getSchema();
        String fullTableName = schema != null && !schema.isEmpty() ?
                schema + "." + backupTable : backupTable;

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(fullTableName).append(" (");

        // Get list of columns dynamically based on the entity
        // This is simplified - in a real implementation you'd need to determine column lists
        sb.append("backup_id, task_id, entity_id, backup_time, reinstated, original_table, backup_data");

        sb.append(") SELECT ");

        // For each source column, map it to the destination column
        sb.append("'").append(UUID.randomUUID()).append("', "); // backup_id
        sb.append("'").append(taskId).append("', "); // task_id
        sb.append("e.id, "); // entity_id
        sb.append("CURRENT_TIMESTAMP, "); // backup_time
        sb.append("FALSE, "); // reinstated
        sb.append("'").append(entityConfig.getTable()).append("', "); // original_table

        // Generate JSON backup data containing all columns
        sb.append("to_jsonb(e) "); // backup_data (PostgreSQL syntax)

        sb.append("FROM ").append(entityConfig.getTable()).append(" e ");

        // Add the same WHERE clause as the find query
        String findQuery = buildFindCandidatesQuery(entityConfig);
        int whereIndex = findQuery.indexOf("WHERE");
        if (whereIndex > 0) {
            sb.append(findQuery.substring(whereIndex));
        } else {
            sb.append("WHERE 1=1");
        }

        return sb.toString();
    }

    private String buildDirectDeleteQuery(EntityConfig entityConfig) {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(entityConfig.getTable()).append(" e ");

        // Add the same WHERE clause as the find query
        String findQuery = buildFindCandidatesQuery(entityConfig);
        int whereIndex = findQuery.indexOf("WHERE");
        if (whereIndex > 0) {
            sb.append(findQuery.substring(whereIndex));
        } else {
            sb.append("WHERE 1=1");
        }

        return sb.toString();
    }

    private String buildRelatedDeleteQuery(EntityConfig parentConfig, RelatedEntityConfig relatedConfig) {
        EntityConfig relatedEntityConfig = entityConfigMap.get(relatedConfig.getEntity());
        if (relatedEntityConfig == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(relatedConfig.getTable()).append(" WHERE ");

        // If a foreign key is specified, use that, otherwise use the join field
        String foreignKeyField = relatedConfig.getForeignKey();
        if (foreignKeyField == null || foreignKeyField.isEmpty()) {
            foreignKeyField = relatedConfig.getJoin();
        }

        sb.append(foreignKeyField).append(" IN (");

        // Sub-select to get parent ids
        sb.append("SELECT e.id FROM ").append(parentConfig.getTable()).append(" e");

        // Add the same WHERE clause as the parent find query
        String findQuery = buildFindCandidatesQuery(parentConfig);
        int whereIndex = findQuery.indexOf("WHERE");
        if (whereIndex > 0) {
            sb.append(" ").append(findQuery.substring(whereIndex));
        } else {
            sb.append(" WHERE 1=1");
        }

        sb.append(")");

        return sb.toString();
    }

    private String buildReinstateQuery(EntityConfig entityConfig) {
        String backupTable = entityConfig.getBackup().getTable();
        String schema = entityConfig.getBackup().getSchema();
        String fullBackupTable = schema != null && !schema.isEmpty() ?
                schema + "." + backupTable : backupTable;

        String sb = "INSERT INTO " + entityConfig.getTable() + " (" +

                // TODO: Build column list dynamically based on entity
                "id /* Add all columns from original table */" +
                ") SELECT " +
                "entity_id /* Extract columns from backup_data JSON */ " +
                "FROM " + fullBackupTable +
                " WHERE backup_id IN (:backupIds) AND reinstated = FALSE";

        return sb;
    }

    private String buildMarkReinstatedQuery(EntityConfig entityConfig) {
        String backupTable = entityConfig.getBackup().getTable();
        String schema = entityConfig.getBackup().getSchema();
        String fullBackupTable = schema != null && !schema.isEmpty() ?
                schema + "." + backupTable : backupTable;

        String sb = "UPDATE " + fullBackupTable +
                " SET reinstated = TRUE, " +
                "reinstated_time = CURRENT_TIMESTAMP, " +
                "reinstated_by = :username " +
                "WHERE backup_id IN (:backupIds)";

        return sb;
    }

    private RelatedEntityConfig findRelationshipBetween(String entity1, String entity2) {
        // Look up configs for both entities
        EntityConfig config1 = entityConfigMap.get(entity1);
        EntityConfig config2 = entityConfigMap.get(entity2);

        if (config1 == null || config2 == null) {
            return null;
        }

        // Check if entity1 has a relationship to entity2
        if (config1.getRelated() != null) {
            for (RelatedEntityConfig relation : config1.getRelated()) {
                if (relation.getEntity().equals(entity2)) {
                    return relation;
                }
            }
        }

        // Check if entity2 has a relationship to entity1
        if (config2.getRelated() != null) {
            for (RelatedEntityConfig relation : config2.getRelated()) {
                if (relation.getEntity().equals(entity1)) {
                    return relation;
                }
            }
        }

        return null;
    }
}