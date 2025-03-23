package org.example.dbcleanup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "cleanupTaskExecutor")
    public Executor cleanupTaskExecutor(CleanupProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int workerCount = properties.getDistribution() != null ?
                properties.getDistribution().getWorkerCount() : 4;

        executor.setCorePoolSize(workerCount);
        executor.setMaxPoolSize(workerCount);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cleanup-executor-");
        executor.initialize();
        return executor;
    }
}