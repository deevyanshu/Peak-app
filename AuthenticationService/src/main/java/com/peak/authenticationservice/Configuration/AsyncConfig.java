package com.peak.authenticationservice.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

//    @Bean(name="emailTaskExecutor")
//    public Executor taskExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(5);       // Minimum number of alive threads
//        executor.setMaxPoolSize(10);       // Max threads allowed in pool
//        executor.setQueueCapacity(100);    // Queue capacity before growing pool
//        executor.setThreadNamePrefix("AsyncThread-");
//        executor.initialize();
//        return executor;
//    }

    @Bean(name="emailTaskExecutor")
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
