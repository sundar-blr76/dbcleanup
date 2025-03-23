package org.example.dbcleanup.controller;

import org.example.dbcleanup.model.CleanupResult;
import org.example.dbcleanup.service.CleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/cleanup")
public class CleanupController {
    private static final Logger logger = LoggerFactory.getLogger(CleanupController.class);

    private final CleanupService cleanupService;

    public CleanupController(CleanupService cleanupService) {
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

        logger.info("Analyzing cleanup candidates, initiator: {}", initiator);
        return ResponseEntity.ok(cleanupService.analyzeCleanupCandidates(initiator));
    }

    @PostMapping("/execute")
    public ResponseEntity<CleanupResult> executeCleanup(
            Principal principal,
            @RequestParam(required = false, defaultValue = "false") boolean dryRun,
            @RequestParam(required = false, defaultValue = "api") String initiator) {

        // If we have authenticated user, use that as initiator
        if (principal != null) {
            initiator = principal.getName();
        }

        logger.info("Executing cleanup, dryRun: {}, initiator: {}", dryRun, initiator);
        return ResponseEntity.ok(cleanupService.executeCleanup(initiator, dryRun));
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

        logger.info("Reinstating {} backups for entity {}, initiator: {}",
                backupIds.size(), entityName, initiator);
        int count = cleanupService.reinstateBackups(entityName, backupIds, initiator);
        return ResponseEntity.ok(count);
    }
}