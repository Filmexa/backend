package com.filmexa.stream.modules.download.config;

import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.runtime.BtRuntime;
import bt.runtime.Config;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One BitTorrent runtime shared by every download.
 *
 * DHT binds a single UDP port and keeps one routing table, so it has to live
 * outside the per-movie clients: a runtime per download would make each one
 * fight for the same port and bootstrap its own routing table from scratch.
 */
@Configuration
@Slf4j
public class TorrentRuntimeConfig {

    @Value("${app.torrent.acceptor-port:6891}")
    private int acceptorPort;

    @Value("${app.torrent.dht-port:49001}")
    private int dhtPort;

    private BtRuntime runtime;

    @Bean
    public BtRuntime btRuntime() {
        // Our magnets carry only an info hash - no trackers - so DHT is the only
        // way to find peers, and it needs the public routers to join the network.
        DHTConfig dhtConfig = new DHTConfig();
        dhtConfig.setShouldUseRouterBootstrap(true);
        dhtConfig.setListeningPort(dhtPort);

        Config config = new Config();
        config.setAcceptorPort(acceptorPort);

        runtime = BtRuntime.builder(config)
            .module(new DHTModule(dhtConfig))
            .autoLoadModules()
            .disableAutomaticShutdown()
            .build();

        runtime.startup();
        log.info("BitTorrent runtime started (acceptor port {}, DHT port {})", acceptorPort, dhtPort);
        return runtime;
    }

    @PreDestroy
    public void shutdown() {
        if (runtime != null) {
            runtime.shutdown();
        }
    }
}
