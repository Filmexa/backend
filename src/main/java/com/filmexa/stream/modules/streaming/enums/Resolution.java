package com.filmexa.stream.modules.streaming.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public enum Resolution {

    P144("144p", 144, 200, 64),
    P240("240p", 240, 400, 64),
    P360("360p", 360, 800, 96),
    P480("480p", 480, 1400, 96),
    P720("720p", 720, 2800, 128),
    P1080("1080p", 1080, 5000, 128);

    private final String label;
    private final int height;
    private final int videoBitrateKbps;
    private final int audioBitrateKbps;

    Resolution(String label, int height, int videoBitrateKbps, int audioBitrateKbps) {
        this.label = label;
        this.height = height;
        this.videoBitrateKbps = videoBitrateKbps;
        this.audioBitrateKbps = audioBitrateKbps;
    }

    public String getLabel() {
        return label;
    }

    public int getHeight() {
        return height;
    }

    public int getVideoBitrateKbps() {
        return videoBitrateKbps;
    }

    public int getAudioBitrateKbps() {
        return audioBitrateKbps;
    }

    /** Advertised HLS bandwidth, with a little headroom for container overhead. */
    public int getTotalBitrateBps() {
        return (int) ((videoBitrateKbps + audioBitrateKbps) * 1000 * 1.1);
    }

    /**
     * The rungs to offer for a source of the given height: everything at or below it.
     * A source smaller than the lowest rung still gets that lowest rung, otherwise
     * a 240p file would have no variants at all.
     */
    public static List<Resolution> ladderFor(int sourceHeight) {
        List<Resolution> rungs = new ArrayList<>();
        for (Resolution resolution : values()) {
            if (resolution.height <= sourceHeight) {
                rungs.add(resolution);
            }
        }
        if (rungs.isEmpty()) {
            rungs.add(P360);
        }
        rungs.sort(Comparator.comparingInt(Resolution::getHeight));
        return rungs;
    }

    public static Optional<Resolution> byHeight(int height) {
        return Arrays.stream(values())
                .filter(resolution -> resolution.height == height)
                .findFirst();
    }
}
