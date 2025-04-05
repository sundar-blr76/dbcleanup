package com.dbcleanup.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CleanupProperties.class)
public class CleanupConfig {

    public CleanupConfig() {
        super();
    }
} 