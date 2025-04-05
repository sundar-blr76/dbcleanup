// src/test/java/com/dbcleanup/service/CleanupServiceTest.java
package com.dbcleanup.service;

import com.dbcleanup.config.CleanupProperties;
import com.dbcleanup.config.CleanupProperties.EntityConfig;
import com.dbcleanup.model.CleanupResult;
import com.dbcleanup.repository.CleanupRepository;
import com.dbcleanup.repository.TaskLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CleanupServiceTest {

    @Mock
    private CleanupProperties properties;

    @Mock
    private CleanupRepository cleanupRepository;

    @Mock
    private TaskLogRepository taskLogRepository;

    @Mock
    private DistributedCleanupService distributedCleanupService;

    private CleanupService cleanupService;

    public CleanupServiceTest() {
        super();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cleanupService = new CleanupService(
                properties, cleanupRepository, taskLogRepository, distributedCleanupService);
    }

    @Test
    public void shouldReturnCandidatesWhenAnalyzingCleanup() {
        // Arrange
        EntityConfig entity1 = new EntityConfig();
        entity1.setName("Order");
        entity1.setTable("orders");

        EntityConfig entity2 = new EntityConfig();
        entity2.setName("Customer");
        entity2.setTable("customers");

        when(properties.getEntities()).thenReturn(Arrays.asList(entity1, entity2));
        when(taskLogRepository.logTaskStart(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn("task-id-123");

        when(cleanupRepository.findCandidateIds(entity1))
                .thenReturn(Arrays.asList("order-1", "order-2"));
        when(cleanupRepository.findCandidateIds(entity2))
                .thenReturn(Collections.singletonList("customer-1"));

        // Act
        CleanupResult result = cleanupService.analyzeCleanupCandidates("testUser");

        // Assert
        assertNotNull(result);
        assertEquals("task-id-123", result.getTaskId());
        assertEquals(2, result.getCandidateIds("Order").size());
        assertEquals(1, result.getCandidateIds("Customer").size());
        assertEquals(3, result.getTotalCandidateCount());

        verify(taskLogRepository).logTaskStart(eq("ANALYSIS"), eq("testUser"), anyList(), eq(true));
        verify(taskLogRepository).logTaskCompletion(eq("task-id-123"), eq(3), eq(0));
    }

    @Test
    public void shouldNotDeleteOrBackupWhenExecutingCleanupInDryRun() {
        // Arrange
        EntityConfig entity = new EntityConfig();
        entity.setName("Order");
        entity.setTable("orders");
        CleanupProperties.BackupConfig backupConfig = new CleanupProperties.BackupConfig();
        backupConfig.setEnabled(true);
        backupConfig.setTable("orders_backup");
        entity.setBackup(backupConfig);

        when(properties.getEntities()).thenReturn(Collections.singletonList(entity));
        when(properties.getDistribution()).thenReturn(null);

        when(taskLogRepository.logTaskStart(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn("task-id-123");

        List<String> candidateIds = Arrays.asList("order-1", "order-2");
        when(cleanupRepository.findCandidateIds(entity)).thenReturn(candidateIds);

        // Act
        CleanupResult result = cleanupService.executeCleanup("testUser", true);

        // Assert
        assertNotNull(result);
        assertEquals("task-id-123", result.getTaskId());
        assertEquals(2, result.getCandidateIds("Order").size());

        verify(cleanupRepository, never()).backupCandidatesDirect(any(), anyString());
        verify(cleanupRepository, never()).deleteCandidatesDirect(any());
        verify(taskLogRepository).logTaskCompletion(eq("task-id-123"), eq(2), eq(0));
    }

    @Test
    public void shouldBackupAndDeleteWhenExecutingCleanupNotInDryRun() {
        // Arrange
        EntityConfig entity = new EntityConfig();
        entity.setName("Order");
        entity.setTable("orders");
        CleanupProperties.BackupConfig backupConfig = new CleanupProperties.BackupConfig();
        backupConfig.setEnabled(true);
        backupConfig.setTable("orders_backup");
        entity.setBackup(backupConfig);

        when(properties.getEntities()).thenReturn(Collections.singletonList(entity));
        when(properties.getDistribution()).thenReturn(null);

        when(taskLogRepository.logTaskStart(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn("task-id-123");

        List<String> candidateIds = Arrays.asList("order-1", "order-2");
        when(cleanupRepository.findCandidateIds(entity)).thenReturn(candidateIds);
        when(cleanupRepository.backupCandidatesDirect(entity, "task-id-123")).thenReturn(2);
        when(cleanupRepository.deleteCandidatesDirect(entity)).thenReturn(2);

        // Act
        CleanupResult result = cleanupService.executeCleanup("testUser", false);

        // Assert
        assertNotNull(result);
        assertEquals("task-id-123", result.getTaskId());
        assertEquals(2, result.getCandidateIds("Order").size());
        assertEquals(2, result.getBackedUpCount("Order"));
        assertEquals(2, result.getDeletedCount("Order"));

        verify(cleanupRepository).backupCandidatesDirect(entity, "task-id-123");
        verify(cleanupRepository).deleteCandidatesDirect(entity);
        verify(taskLogRepository).logTaskCompletion(eq("task-id-123"), eq(2), eq(2));
    }
}
