package org.example.dbcleanup.controller;

import org.example.dbcleanup.model.CleanupTask;
import org.example.dbcleanup.service.CleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cleanup/tasks")
public class TaskLogController {
    private static final Logger logger = LoggerFactory.getLogger(TaskLogController.class);

    private final CleanupService cleanupService;

    public TaskLogController(CleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @GetMapping
    public ResponseEntity<List<CleanupTask>> getRecentTasks(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        logger.info("Getting {} recent tasks", limit);
        return ResponseEntity.ok(cleanupService.getRecentTasks(limit));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<CleanupTask> getTask(@PathVariable String taskId) {
        logger.info("Getting task with ID: {}", taskId);
        return ResponseEntity.ok(cleanupService.getTask(taskId));
    }
}