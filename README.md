# Advanced Database Cleanup Tool

A sophisticated Spring Boot application designed for intelligent database cleanup operations in production environments. This tool provides configurable, safe, and consistent cleanup mechanisms with backup and restore capabilities.

## Features

### Core Functionality
- Entity-level cleanup configuration
- Parent-child relationship handling with referential integrity
- Configurable cleanup criteria per entity
- Backup and restore mechanisms
- Comprehensive logging and monitoring
- Cron-based automated cleanup
- REST API endpoints for manual operations

### Safety Features
- Maximum cleanup percentage limits
- Runtime duration limits
- Pre-cleanup backup of candidate records
- Restore capability for backed-up data
- Transaction management for data consistency
- Validation of cleanup operations

### Monitoring & Logging
- Detailed operation logs
- Cleanup impact analysis
- Performance metrics
- Status tracking
- Audit trail

## Architecture

### Configuration Structure
```yaml
cleanup:
  entities:
    - name: "EntityName"
      criteria:
        - whereClause: "status = :status AND created_date < :createdDate AND amount > :amount"
          parameters:
            - name: "status"
              value: "COMPLETED"
              type: "STRING"
            - name: "createdDate"
              value: "2024-01-01"
              type: "DATE"
            - name: "amount"
              value: 1000
              type: "DECIMAL"
      maxCleanupPercentage: 20
      maxRuntimeMinutes: 60
      relationships:
        - type: "CHILD"
          entity: "ChildEntity"
          foreignKey: "parentId"
      backup:
        enabled: true
        retentionDays: 30
```

### Components
1. **Configuration Service**
   - Manages entity-level cleanup rules
   - Handles relationship definitions
   - Validates cleanup criteria

2. **Cleanup Service**
   - Executes cleanup operations
   - Manages transaction boundaries
   - Enforces runtime limits
   - Handles backup operations

3. **Backup Service**
   - Creates backups of candidate records
   - Manages backup retention
   - Handles restore operations

4. **Scheduler Service**
   - Manages cron-based cleanup
   - Handles concurrent cleanup operations
   - Implements cleanup throttling

5. **Monitoring Service**
   - Tracks cleanup operations
   - Generates impact reports
   - Manages audit logs

## API Endpoints

### Cleanup Operations
```
POST /api/cleanup/execute
POST /api/cleanup/execute/{entityName}
POST /api/cleanup/simulate
POST /api/cleanup/simulate/{entityName}
```

### Backup & Restore
```
POST /api/cleanup/backup/{entityName}
POST /api/cleanup/restore/{backupId}
GET /api/cleanup/backups
```

### Configuration
```
GET /api/cleanup/config
PUT /api/cleanup/config
GET /api/cleanup/config/{entityName}
PUT /api/cleanup/config/{entityName}
```

### Monitoring
```
GET /api/cleanup/status
GET /api/cleanup/logs
GET /api/cleanup/metrics
```

## Configuration

### Application Properties
```properties
# Cleanup Configuration
cleanup.scheduler.enabled=true
cleanup.scheduler.cron=0 0 2 * * ?  # Run at 2 AM daily
cleanup.max-concurrent-operations=3
cleanup.default-max-runtime-minutes=120
cleanup.default-max-percentage=25

# Backup Configuration
cleanup.backup.enabled=true
cleanup.backup.location=/path/to/backups
cleanup.backup.default-retention-days=30

# Logging Configuration
cleanup.logging.level=INFO
cleanup.logging.retention-days=90
```

### Entity Configuration Example
```json
{
  "entityName": "Order",
  "criteria": [
    {
      "whereClause": "status = :status AND created_date < :createdDate AND (amount > :minAmount OR customer_type = :customerType)",
      "parameters": [
        {
          "name": "status",
          "value": "COMPLETED",
          "type": "STRING"
        },
        {
          "name": "createdDate",
          "value": "2024-01-01",
          "type": "DATE"
        },
        {
          "name": "minAmount",
          "value": 1000,
          "type": "DECIMAL"
        },
        {
          "name": "customerType",
          "value": "PREMIUM",
          "type": "STRING"
        }
      ]
    }
  ],
  "maxCleanupPercentage": 20,
  "maxRuntimeMinutes": 60,
  "relationships": [
    {
      "type": "CHILD",
      "entity": "OrderItem",
      "foreignKey": "orderId"
    }
  ],
  "backup": {
    "enabled": true,
    "retentionDays": 30
  }
}
```

### Complex Query Examples

1. **Date Range with Multiple Conditions**
```json
{
  "whereClause": "created_date BETWEEN :startDate AND :endDate AND status IN (:statuses) AND region = :region",
  "parameters": [
    {
      "name": "startDate",
      "value": "2023-01-01",
      "type": "DATE"
    },
    {
      "name": "endDate",
      "value": "2024-01-01",
      "type": "DATE"
    },
    {
      "name": "statuses",
      "value": ["COMPLETED", "CANCELLED", "EXPIRED"],
      "type": "STRING_ARRAY"
    },
    {
      "name": "region",
      "value": "EUROPE",
      "type": "STRING"
    }
  ]
}
```

2. **Subquery with Aggregation**
```json
{
  "whereClause": "id IN (SELECT order_id FROM order_items GROUP BY order_id HAVING COUNT(*) > :maxItems)",
  "parameters": [
    {
      "name": "maxItems",
      "value": 10,
      "type": "INTEGER"
    }
  ]
}
```

3. **Complex Business Logic**
```json
{
  "whereClause": "(status = 'PENDING' AND created_date < :oldDate) OR (status = 'PROCESSING' AND last_updated < :staleDate) OR (amount > :threshold AND customer_type = 'VIP')",
  "parameters": [
    {
      "name": "oldDate",
      "value": "2024-01-01",
      "type": "DATE"
    },
    {
      "name": "staleDate",
      "value": "2024-02-01",
      "type": "DATE"
    },
    {
      "name": "threshold",
      "value": 5000,
      "type": "DECIMAL"
    }
  ]
}
```

### Parameter Types Supported
- STRING
- INTEGER
- DECIMAL
- DATE
- TIMESTAMP
- BOOLEAN
- STRING_ARRAY
- INTEGER_ARRAY
- DECIMAL_ARRAY
- DATE_ARRAY

### Safety Features for Custom Where Clauses
1. **SQL Injection Prevention**
   - All parameters are properly escaped
   - Parameter binding using prepared statements
   - Input validation and sanitization

2. **Query Performance**
   - Maximum query complexity limits
   - Query execution time monitoring
   - Index usage validation

3. **Validation**
   - Syntax checking
   - Table and column existence verification
   - Parameter type validation

## Usage Examples

### Execute Cleanup
```bash
curl -X POST http://localhost:8080/api/cleanup/execute/Order \
  -H "Content-Type: application/json" \
  -d '{
    "batchSize": 1000,
    "dryRun": false
  }'
```

### Simulate Cleanup
```bash
curl -X POST http://localhost:8080/api/cleanup/simulate/Order \
  -H "Content-Type: application/json" \
  -d '{
    "batchSize": 1000
  }'
```

### Restore Backup
```bash
curl -X POST http://localhost:8080/api/cleanup/restore/backup-123 \
  -H "Content-Type: application/json" \
  -d '{
    "validateOnly": false
  }'
```

## Monitoring & Logging

### Log Structure
```json
{
  "timestamp": "2024-03-28T10:00:00Z",
  "operationId": "cleanup-123",
  "entity": "Order",
  "status": "COMPLETED",
  "recordsProcessed": 1000,
  "recordsDeleted": 950,
  "backupCreated": true,
  "backupId": "backup-123",
  "duration": "PT5M30S",
  "error": null
}
```

### Metrics
- Cleanup operation duration
- Records processed/deleted
- Backup size
- Error rates
- Concurrent operations

## Best Practices

1. **Before Cleanup**
   - Review cleanup criteria
   - Verify backup configuration
   - Check system resources
   - Review related entities

2. **During Cleanup**
   - Monitor operation progress
   - Watch for error rates
   - Check backup creation
   - Monitor system resources

3. **After Cleanup**
   - Verify backup integrity
   - Review cleanup logs
   - Check related entities
   - Update monitoring metrics

## Error Handling

The application implements comprehensive error handling:
- Validation errors
- Database constraints
- Resource limitations
- Backup failures
- Restore failures

## Security Considerations

1. **Access Control**
   - Role-based access
   - Operation authentication
   - API security

2. **Data Protection**
   - Backup encryption
   - Secure storage
   - Access logging

3. **Audit Trail**
   - Operation tracking
   - User tracking
   - Change history

## Development Setup

### Prerequisites
- Java 17+
- Gradle 8.5+
- Database (PostgreSQL/MySQL recommended)
- Redis (for distributed locking)

### Building
```bash
./gradlew clean build
```

### Running
```bash
./gradlew bootRun
```

### Testing
```bash
./gradlew test
```

### Code Quality Checks
```bash
./gradlew checkstyleMain checkstyleTest
./gradlew jacocoTestReport
```

### Publishing
```bash
./gradlew publish
```

## Contributing

Please read CONTRIBUTING.md for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

### Scenarios

1. **Pre-trade Rule Log Cleanup**
```yaml
cleanup:
  entities:
    - name: "rule_log"
      criteria:
        - whereClause: "check_date < :expirationDate AND status = :status"
          parameters:
            - name: "expirationDate"
              value: "2024-01-01"
              type: "DATE"
            - name: "status"
              value: "COMPLETED"
              type: "STRING"
      maxCleanupPercentage: 25
      maxRuntimeMinutes: 60
      relationships:
        - type: "CHILD"
          entity: "rule_log_contribution"
          foreignKey: "check_id"
      backup:
        enabled: true
        retentionDays: 90

    - name: "rule_log_contribution"
      criteria:
        - whereClause: "check_id IN (SELECT check_id FROM rule_log WHERE check_date < :expirationDate AND status = :status)"
          parameters:
            - name: "expirationDate"
              value: "2024-01-01"
              type: "DATE"
            - name: "status"
              value: "COMPLETED"
              type: "STRING"
      maxCleanupPercentage: 25
      maxRuntimeMinutes: 30
      backup:
        enabled: true
        retentionDays: 90
```

2. **Synthetic Account Cleanup After Exchange Fund Transfer**
```yaml
cleanup:
  entities:
    - name: "account"
      criteria:
        - whereClause: "id = :accountId AND exchange_fund_transfer_status = :transferStatus"
          parameters:
            - name: "accountId"
              value: "12345"
              type: "INTEGER"
            - name: "transferStatus"
              value: "COMPLETED"
              type: "STRING"
      maxCleanupPercentage: 20
      maxRuntimeMinutes: 120
      relationships:
        - type: "CHILD"
          entity: "node"
          foreignKey: "account_id"
        - type: "CHILD"
          entity: "position_overnight"
          foreignKey: "account_id"
      backup:
        enabled: true
        retentionDays: 30

    - name: "node"
      criteria:
        - whereClause: "account_id = :accountId"
          parameters:
            - name: "accountId"
              value: "12345"
              type: "INTEGER"
      maxCleanupPercentage: 20
      maxRuntimeMinutes: 60
      relationships:
        - type: "CHILD"
          entity: "node_parent_shortcut"
          foreignKey: "node_id"
        - type: "CHILD"
          entity: "node_shortcut_set"
          foreignKey: "node_id"
      backup:
        enabled: true
        retentionDays: 30

    - name: "node_parent_shortcut"
      criteria:
        - whereClause: "node_id IN (SELECT id FROM node WHERE account_id = :accountId)"
          parameters:
            - name: "accountId"
              value: "12345"
              type: "INTEGER"
      maxCleanupPercentage: 20
      maxRuntimeMinutes: 30
      backup:
        enabled: true
        retentionDays: 30

    - name: "node_shortcut_set"
      criteria:
        - whereClause: "node_id IN (SELECT id FROM node WHERE account_id = :accountId)"
          parameters:
            - name: "accountId"
              value: "12345"
              type: "INTEGER"
      maxCleanupPercentage: 20
      maxRuntimeMinutes: 30
      backup:
        enabled: true
        retentionDays: 30

    - name: "position_overnight"
      criteria:
        - whereClause: "account_id = :accountId"
          parameters:
            - name: "accountId"
              value: "12345"
              type: "INTEGER"
      maxCleanupPercentage: 20
      maxRuntimeMinutes: 30
      backup:
        enabled: true
        retentionDays: 30
```

### Parameter Types Supported

// ... rest of existing code ... 