package com.example.sonata_brain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Runs every dues rebuild, one at a time. A single thread means two rebuilds never race over the same student's
     * dues, and the order of requests is kept.
     */
    @Bean(name = "duesExecutor")
    public Executor duesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10_000);
        executor.setThreadNamePrefix("dues-");
        executor.initialize();
        return executor;
    }
}
