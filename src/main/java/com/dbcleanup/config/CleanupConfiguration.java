package com.dbcleanup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CleanupConfiguration {
    
    public CleanupConfiguration() {
        super();
    }
    
    @Bean
    public CleanupProperties.TaskLoggingConfig taskLoggingConfig(CleanupProperties cleanupProperties) {
        return cleanupProperties.getTaskLogging();
    }
} 
