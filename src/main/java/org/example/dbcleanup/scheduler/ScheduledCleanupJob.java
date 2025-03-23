package org.example.dbcleanup.scheduler;

import org.example.dbcleanup.config.CleanupProperties;
import org.example.dbcleanup.service.CleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "cleanup.scheduler", name = "enabled", havingValue = "true")
public class ScheduledCleanupJob {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledCleanupJob.class);

    private final CleanupService cleanupService;
    private final CleanupProperties properties;

    public ScheduledCleanupJob(CleanupService cleanupService, CleanupProperties properties) {
        this.cleanupService = cleanupService;
        this.properties = properties;
    }

    @Scheduled(cron = "${cleanup.scheduler.cron:0 0 2 * * *}") // Default: 2 AM daily
    public void scheduledCleanup() {
        logger.info("Running scheduled cleanup job");

        boolean dryRun = properties.getScheduler().isDryRun();
        cleanupService.executeCleanup("scheduler", dryRun);
    }
}