package com.filmexa.stream.modules.download.config;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.runtime.BtRuntime;
import bt.runtime.Config;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * One BitTorrent runtime per running download.
 *
 * <p>Sharing a single runtime between downloads does not work: the first magnet resolves
 * and downloads normally, and every torrent started while it is still running waits for
 * its metadata forever - it never gets a single peer, no matter how often its peer lookup
 * is triggered. The same magnet downloads at full speed the moment it is the only torrent
 * in a runtime. bt's peer discovery below the client is effectively runtime-wide and
 * single-threaded, so the way to have several movies downloading at once is to give each
 * one its own runtime.
 *
 * <p>Each runtime needs its own pair of UDP/TCP ports, so slots are handed out from a
 * fixed pool: the slot count caps how many runtimes can exist at once and keeps the port
 * numbers predictable. It matches the download executor's size, so a download always has
 * a slot waiting by the time it gets a thread.
 */
@Component
@Slf4j
public class TorrentRuntimePool {

    @Value("${app.torrent.acceptor-port:6891}")
    private int acceptorPortBase;

    @Value("${app.torrent.dht-port:49001}")
    private int dhtPortBase;

    @Value("${app.torrent.max-concurrent-downloads:6}")
    private int maxConcurrentDownloads;

    /** Peer connections for a runtime that now serves exactly one torrent. */
    @Value("${app.torrent.max-peer-connections-per-download:100}")
    private int maxPeerConnectionsPerDownload;

    @Value("${app.torrent.hashing-threads:2}")
    private int hashingThreads;

    private BlockingQueue<Integer> freeSlots;

    private final Map<Integer, BtRuntime> runtimesBySlot = new ConcurrentHashMap<>();

    @PostConstruct
    void createSlots() {
        int slots = Math.max(1, maxConcurrentDownloads);
        freeSlots = new ArrayBlockingQueue<>(slots);
        for (int slot = 0; slot < slots; slot++) {
            freeSlots.add(slot);
        }
        log.info("Torrent runtime pool ready: {} slots, acceptor ports {}-{}, DHT ports {}-{}",
                slots, acceptorPortBase, acceptorPortBase + slots - 1,
                dhtPortBase, dhtPortBase + slots - 1);
    }

    /**
     * Starts a runtime for one download. The caller must {@link #release(Lease)} it when
     * the download ends, otherwise the slot and its ports stay taken.
     *
     * @throws IllegalStateException if every slot is in use
     */
    public Lease acquire(Long movieId) {
        Integer slot = freeSlots.poll();
        if (slot == null) {
            throw new IllegalStateException("No free torrent runtime slot for movie " + movieId);
        }

        try {
            BtRuntime runtime = startRuntime(slot);
            runtimesBySlot.put(slot, runtime);
            return new Lease(slot, runtime);
        } catch (RuntimeException e) {
            freeSlots.offer(slot);
            throw e;
        }
    }

    public void release(Lease lease) {
        if (lease == null) {
            return;
        }

        BtRuntime runtime = runtimesBySlot.remove(lease.slot());
        if (runtime != null) {
            try {
                runtime.shutdown();
            } catch (Exception e) {
                log.warn("Torrent runtime on slot {} did not shut down cleanly: {}",
                        lease.slot(), e.getMessage());
            }
        }
        freeSlots.offer(lease.slot());
        log.info("Released torrent runtime slot {}", lease.slot());
    }

    private BtRuntime startRuntime(int slot) {
        int acceptorPort = acceptorPortBase + slot;
        int dhtPort = dhtPortBase + slot;

        // Our magnets carry only an info hash and udp:// trackers, which bt has no client
        // for - DHT is the only way to find peers, and it needs the public routers to
        // join the network.
        DHTConfig dhtConfig = new DHTConfig();
        dhtConfig.setShouldUseRouterBootstrap(true);
        dhtConfig.setListeningPort(dhtPort);

        Config config = new Config();
        config.setAcceptorPort(acceptorPort);
        config.setMaxPeerConnections(maxPeerConnectionsPerDownload);
        config.setMaxPeerConnectionsPerTorrent(maxPeerConnectionsPerDownload);
        config.setNumOfHashingThreads(hashingThreads);

        InetAddress outbound = outboundAddress();
        if (outbound != null) {
            config.setAcceptorAddress(outbound);
        }

        BtRuntime runtime = BtRuntime.builder(config)
                .module(new DHTModule(dhtConfig))
                .autoLoadModules()
                .disableAutomaticShutdown()
                .build();

        runtime.startup();
        log.info("Started torrent runtime on slot {} (acceptor port {}, DHT port {})",
                slot, acceptorPort, dhtPort);
        return runtime;
    }

    private InetAddress outboundAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress();
        } catch (Exception e) {
            log.warn("Could not determine outbound network interface: {}", e.getMessage());
            return null;
        }
    }

    @PreDestroy
    void shutdownAll() {
        runtimesBySlot.forEach((slot, runtime) -> {
            try {
                runtime.shutdown();
            } catch (Exception e) {
                log.warn("Torrent runtime on slot {} did not shut down cleanly: {}", slot, e.getMessage());
            }
        });
        runtimesBySlot.clear();
    }

    /** A running runtime and the port slot it occupies. */
    public record Lease(int slot, BtRuntime runtime) {
    }
}
