package com.dbcleanup.controller;

import com.dbcleanup.model.CleanupTask;
import com.dbcleanup.service.CleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskLogController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskLogController.class);

    private final CleanupService cleanupService;

    public TaskLogController(CleanupService cleanupService) {
        super();
        this.cleanupService = cleanupService;
    }

    @GetMapping
    public ResponseEntity<List<CleanupTask>> getRecentTasks(@RequestParam(required = false, defaultValue = "10") int limit) {
        LOGGER.info("Getting {} recent tasks", limit);
        return ResponseEntity.ok(cleanupService.getRecentTasks(limit));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<CleanupTask> getTask(@PathVariable String taskId) {
        LOGGER.info("Getting task with ID: {}", taskId);
        return ResponseEntity.ok(cleanupService.getTask(taskId));
    }
}
