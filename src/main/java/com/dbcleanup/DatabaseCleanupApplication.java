package com.dbcleanup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class DatabaseCleanupApplication {
    public DatabaseCleanupApplication() {
        super();
    }
    public static void main(String[] args) {
        SpringApplication.run(DatabaseCleanupApplication.class, args);
    }
}
