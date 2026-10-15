package com.example.sonata_brain.config;

import com.example.sonata_brain.common.context.ContextCopyingTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling // nightly dues recalculation (REQ-074.AC9)
public class AsyncConfig {

    @Bean(name = "duesEventExecutor")
    public Executor duesEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("DuesEvent-");
        executor.setTaskDecorator(new ContextCopyingTaskDecorator()); // keep the request context (REQ-008.AC5)
        executor.initialize();
        return executor;
    }
}
