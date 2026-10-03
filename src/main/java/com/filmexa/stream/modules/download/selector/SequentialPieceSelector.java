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

    /** Inclusive piece range occupied by the actual movie inside a multi-file torrent. */
    private volatile int movieFirstPiece = 0;
    private volatile int movieLastPiece = -1;
    private volatile boolean movieRangeConfigured;

    /** Where playback is, in pieces. Everything from here on is wanted first. */
    private volatile int playheadPiece = 0;

    public SequentialPieceSelector(boolean isMp4) {
        this.isMp4 = isMp4;
    }

    @Override
    public void initSelector(int totalPieces) {
        this.totalPieces = totalPieces;
        if (movieLastPiece < 0) {
            movieLastPiece = totalPieces - 1;
        }
    }

    /**
     * Supplies the movie's real piece range once torrent metadata is available.
     * Fractions used by playback must be relative to this range, not to samples,
     * subtitles and other files that happen to share the torrent.
     */
    public void configureMovieRange(int firstPiece, int lastPiece, boolean mp4) {
        if (totalPieces <= 0) {
            return;
        }
        int first = Math.max(0, Math.min(firstPiece, totalPieces - 1));
        int last = Math.max(first, Math.min(lastPiece, totalPieces - 1));
        this.movieFirstPiece = first;
        this.movieLastPiece = last;
        this.isMp4 = mp4;
        this.movieRangeConfigured = true;
        if (playheadPiece < first || playheadPiece > last) {
            this.playheadPiece = first;
        }
    }

    public boolean isMovieRangeConfigured() {
        return movieRangeConfigured;
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
        int first = movieFirstPiece;
        int last = movieLastPiece >= first ? movieLastPiece : pieces - 1;
        int moviePieces = last - first + 1;
        int target = Math.min(last, first + (int) (clamped * moviePieces));
        this.playheadPiece = target;
        return target;
    }

    public int pieceAtFraction(double fraction) {
        int pieces = totalPieces;
        if (pieces <= 0) {
            return 0;
        }
        double clamped = Math.min(1.0, Math.max(0.0, fraction));
        int first = movieFirstPiece;
        int last = movieLastPiece >= first ? movieLastPiece : pieces - 1;
        return Math.min(last, first + (int) (clamped * (last - first + 1)));
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

        // The movie header has to come first whatever the playhead says - piece zero
        // may belong to an nfo or sample in a multi-file torrent.
        // decoded, or even probed, without it.
        int firstPiece = movieFirstPiece;
        if (relevantChunks.get(firstPiece)) {
            builder.add(firstPiece);
        }

        int lastPiece = movieLastPiece >= firstPiece ? movieLastPiece : totalPieces - 1;
        if (isMp4 && totalPieces > 1 && relevantChunks.get(lastPiece)) {
            builder.add(lastPiece);
        }

        int playhead = playheadPiece;

        // From the playhead to the end of the file...
        relevantChunks.stream()
                .filter(i -> i >= playhead)
                .filter(i -> shouldKeepPiece(i, firstPiece, lastPiece))
                .forEach(builder::add);

        // ...then everything skipped over, so a seek does not abandon it for good.
        relevantChunks.stream()
                .filter(i -> i < playhead)
                .filter(i -> shouldKeepPiece(i, firstPiece, lastPiece))
                .forEach(builder::add);

        return builder.build();
    }

    private boolean shouldKeepPiece(int pieceIndex, int firstPiece, int lastPiece) {
        if (pieceIndex == firstPiece) {
            return false; 
        }
        if (isMp4 && pieceIndex == lastPiece) {
            return false; 
        }
        return true; 
    }
}
