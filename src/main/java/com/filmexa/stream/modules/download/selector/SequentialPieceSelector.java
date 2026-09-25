package com.filmexa.stream.modules.download.selector;

import java.util.BitSet;
import java.util.stream.IntStream;

import bt.torrent.PieceStatistics;
import bt.torrent.selector.PieceSelector;

/**
 * Downloads a torrent in playback order rather than the usual rarest-first.
 *
 * <p>Pieces are handed out from the playhead forwards, so whatever the viewer is about to
 * watch arrives first. A seek moves that point: without it, jumping to the last ten
 * minutes of a film would wait for everything in between, which is an hour of downloading
 * for a couple of minutes of video. Pieces before the playhead are still requested, after
 * the ones ahead of it, so skipped stretches fill in while playback continues.
 */
public class SequentialPieceSelector implements PieceSelector {

    private int totalPieces;
    private volatile boolean isMp4;

    /** Where playback is, in pieces. Everything from here on is wanted first. */
    private volatile int playheadPiece = 0;

    public SequentialPieceSelector(boolean isMp4) {
        this.isMp4 = isMp4;
    }

    @Override
    public void initSelector(int totalPieces) {
        this.totalPieces = totalPieces;
    }

    /**
     * Points the download at the part of the film the viewer just seeked to.
     *
     * @param fraction how far into the file, 0.0 to 1.0
     * @return the piece the download will now work from
     */
    public int seekToFraction(double fraction) {
        int pieces = totalPieces;
        if (pieces <= 0) {
            return 0;
        }

        double clamped = Math.min(1.0, Math.max(0.0, fraction));
        int target = Math.min(pieces - 1, (int) (clamped * pieces));
        this.playheadPiece = target;
        return target;
    }

    public int getPlayheadPiece() {
        return playheadPiece;
    }

    @Override
    public IntStream getNextPieces(BitSet relevantChunks, PieceStatistics pieceStatistics) {
        if (relevantChunks.isEmpty()) {
            return IntStream.empty();
        }

        IntStream.Builder builder = IntStream.builder();

        // The header has to come first whatever the playhead says - nothing can be
        // decoded, or even probed, without it.
        if (relevantChunks.get(0)) {
            builder.add(0);
        }

        int lastPiece = totalPieces - 1;
        if (isMp4 && totalPieces > 1 && relevantChunks.get(lastPiece)) {
            builder.add(lastPiece);
        }

        int playhead = playheadPiece;

        // From the playhead to the end of the file...
        relevantChunks.stream()
                .filter(i -> i >= playhead)
                .filter(i -> shouldKeepPiece(i, lastPiece))
                .forEach(builder::add);

        // ...then everything skipped over, so a seek does not abandon it for good.
        relevantChunks.stream()
                .filter(i -> i < playhead)
                .filter(i -> shouldKeepPiece(i, lastPiece))
                .forEach(builder::add);

        return builder.build();
    }

    private boolean shouldKeepPiece(int pieceIndex, int lastPiece) {
        if (pieceIndex == 0) {
            return false; 
        }
        if (isMp4 && pieceIndex == lastPiece) {
            return false; 
        }
        return true; 
    }
}
