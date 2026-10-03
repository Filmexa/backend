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

    /** Headers/indexes regularly span several torrent pieces, not just piece zero. */
    private static final int HEADER_WINDOW_PIECES = 16;

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
        configureMovieRange(firstPiece, lastPiece, mp4 ? "movie.mp4" : "movie.mkv");
    }

    public void configureMovieRange(int firstPiece, int lastPiece, String fileName) {
        if (totalPieces <= 0) {
            return;
        }
        int first = Math.max(0, Math.min(firstPiece, totalPieces - 1));
        int last = Math.max(first, Math.min(lastPiece, totalPieces - 1));
        this.movieFirstPiece = first;
        this.movieLastPiece = last;
        this.isMp4 = needsTailMetadata(fileName);
        this.movieRangeConfigured = true;
        if (playheadPiece < first || playheadPiece > last) {
            this.playheadPiece = first;
        }
    }

    private boolean needsTailMetadata(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".m4v")
                || lower.endsWith(".mov") || lower.endsWith(".3gp")
                || lower.endsWith(".3g2") || lower.endsWith(".mkv")
                || lower.endsWith(".webm");
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

    public int getMovieFirstPiece() {
        return movieFirstPiece;
    }

    @Override
    public IntStream getNextPieces(BitSet relevantChunks, PieceStatistics pieceStatistics) {
        if (relevantChunks.isEmpty()) {
            return IntStream.empty();
        }

        IntStream.Builder builder = IntStream.builder();

        // The complete header window has to come first. MKV/WebM EBML data is at the
        // beginning; AVI and MPEG containers also need their opening structures; and an
        // MP4 moov atom can be much larger than a single torrent piece.
        int firstPiece = movieFirstPiece;
        int lastPiece = movieLastPiece >= firstPiece ? movieLastPiece : totalPieces - 1;
        int headerEnd = Math.min(lastPiece, firstPiece + HEADER_WINDOW_PIECES - 1);
        relevantChunks.stream()
                .filter(i -> i >= firstPiece && i <= headerEnd)
                .forEach(builder::add);

        // ISO-BMFF containers may keep their moov/index at the end; Matroska/WebM often
        // keep seek Cues there too. Fetch a window rather than one last piece because
        // real-world metadata regularly spans several torrent pieces.
        int tailStart = Math.max(firstPiece, lastPiece - HEADER_WINDOW_PIECES + 1);
        if (isMp4) {
            relevantChunks.stream()
                    .filter(i -> i >= tailStart && i <= lastPiece)
                    .filter(i -> i > headerEnd)
                    .forEach(builder::add);
        }

        int playhead = playheadPiece;

        // From the playhead to the end of the file...
        relevantChunks.stream()
                .filter(i -> i >= playhead)
                .filter(i -> shouldKeepPiece(i, firstPiece, headerEnd, tailStart, lastPiece))
                .forEach(builder::add);

        // ...then everything skipped over, so a seek does not abandon it for good.
        relevantChunks.stream()
                .filter(i -> i < playhead)
                .filter(i -> shouldKeepPiece(i, firstPiece, headerEnd, tailStart, lastPiece))
                .forEach(builder::add);

        return builder.build();
    }

    private boolean shouldKeepPiece(int pieceIndex, int firstPiece, int headerEnd,
                                    int tailStart, int lastPiece) {
        boolean inHeader = pieceIndex >= firstPiece && pieceIndex <= headerEnd;
        boolean inTail = isMp4 && pieceIndex >= tailStart && pieceIndex <= lastPiece;
        return !inHeader && !inTail;
    }
}
