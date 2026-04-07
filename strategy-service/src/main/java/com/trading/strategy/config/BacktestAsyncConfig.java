package com.trading.strategy.config;

import java.util.concurrent.Executor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConditionalOnProperty(name = "app.backtest.enabled", havingValue = "true")
public class BacktestAsyncConfig {

    public static final String EXECUTOR = "backtestJobExecutor";

    @Bean(name = EXECUTOR)
    Executor backtestJobExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("backtest-job-");
        ex.initialize();
        return ex;
    }
}
