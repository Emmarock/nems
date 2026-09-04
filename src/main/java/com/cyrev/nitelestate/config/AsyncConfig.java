package com.cyrev.nitelestate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Backs @Async notification dispatch (see NotificationService) - bounded so broadcasting an
     * announcement to every resident can't spin up an unbounded number of threads the way the
     * default SimpleAsyncTaskExecutor would.
     */
    @Bean
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notif-");
        executor.initialize();
        return executor;
    }
}
