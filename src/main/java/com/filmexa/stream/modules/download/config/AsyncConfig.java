package com.filmexa.stream.modules.download.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * How many movies may download at the same time. Each download holds one thread for
     * its whole lifetime, so this is also the pool size.
     */
    @Value("${app.torrent.max-concurrent-downloads:6}")
    private int maxConcurrentDownloads;

    @Value("${app.torrent.download-queue-capacity:50}")
    private int queueCapacity;

    @Bean(name = "torrentTaskExecutor")
    public Executor torrentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // A ThreadPoolExecutor only grows past the core size once its queue is *full*, so
        // with a 50-slot queue a larger maxPoolSize is never reached: download 3 onwards
        // would sit in the queue behind two downloads that only end when the movie does.
        // Core and max are therefore the same number - the real concurrency limit.
        executor.setCorePoolSize(maxConcurrentDownloads);
        executor.setMaxPoolSize(maxConcurrentDownloads);
        executor.setQueueCapacity(queueCapacity);

        // The threads block on I/O rather than burn CPU, so letting idle ones go keeps
        // the pool from holding maxConcurrentDownloads threads for the whole uptime.
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(120);

        executor.setThreadNamePrefix("torrent-worker-");
        executor.initialize();
        return executor;
    }
}
