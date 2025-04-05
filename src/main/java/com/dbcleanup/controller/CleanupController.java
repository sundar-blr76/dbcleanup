package com.dbcleanup.controller;

import com.dbcleanup.model.CleanupResult;
import com.dbcleanup.service.CleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/cleanup")
public class CleanupController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupController.class);

    private final CleanupService cleanupService;

    public CleanupController(CleanupService cleanupService) {
        super();
        this.cleanupService = cleanupService;
    }

    @GetMapping("/analyze")
    public ResponseEntity<CleanupResult> analyzeCleanup(
            Principal principal,
            @RequestParam(required = false, defaultValue = "api") String initiator) {

        // If we have authenticated user, use that as initiator
        if (principal != null) {
            initiator = principal.getName();
        }

        LOGGER.info("Analyzing cleanup candidates, initiator: {}", initiator);
        return ResponseEntity.ok(cleanupService.analyzeCleanupCandidates(initiator));
    }

    @PostMapping("/execute")
    public ResponseEntity<CleanupResult> executeCleanup(
            @RequestParam(required = false, defaultValue = "false") boolean dryRun) {
        return ResponseEntity.ok(cleanupService.executeCleanup("api", dryRun));
    }

    @PostMapping("/reinstate/{entityName}")
    public ResponseEntity<Integer> reinstateBackups(
            Principal principal,
            @PathVariable String entityName,
            @RequestBody List<String> backupIds,
            @RequestParam(required = false, defaultValue = "api") String initiator) {

        // If we have authenticated user, use that as initiator
        if (principal != null) {
            initiator = principal.getName();
        }

        LOGGER.info("Reinstating {} backups for entity {}, initiator: {}",
                backupIds.size(), entityName, initiator);
        int count = cleanupService.reinstateBackups(entityName, backupIds, initiator);
        return ResponseEntity.ok(count);
    }
}
