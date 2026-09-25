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
    /** Tail pieces of an MP4, where a non-faststart moov atom usually lives. */
    private volatile BitSet prioritizedIndexPieces = new BitSet();
    /** Pieces occupied by the selected video file, which may start after torrent piece 0. */
    private volatile BitSet videoPieces = new BitSet();

    /** Where playback is, in pieces. Everything from here on is wanted first. */
    private volatile int playheadPiece = 0;

    public SequentialPieceSelector() {
    }

    /**
     * Prioritizes the selected MP4's trailing index pieces after the movie header.
     * The torrent metadata is needed to find it; the magnet URI does not contain the
     * video filename reliably.
     */
    public void setIndexPieces(BitSet indexPieces) {
        this.prioritizedIndexPieces = (BitSet) indexPieces.clone();
    }

    /** Sets the selected movie's pieces in global torrent piece coordinates. */
    public void setVideoPieces(BitSet pieces) {
        this.videoPieces = (BitSet) pieces.clone();
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
        BitSet moviePieces = videoPieces;
        if (!moviePieces.isEmpty()) {
            double clamped = Math.min(1.0, Math.max(0.0, fraction));
            int ordinal = Math.min(moviePieces.cardinality() - 1,
                    (int) (clamped * moviePieces.cardinality()));
            int target = moviePieces.nextSetBit(0);
            for (int i = 0; i < ordinal; i++) {
                target = moviePieces.nextSetBit(target + 1);
            }
            this.playheadPiece = target;
            return target;
        }

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
        BitSet moviePieces = videoPieces;

        if (!moviePieces.isEmpty()) {
            int headerPiece = moviePieces.nextSetBit(0);
            if (relevantChunks.get(headerPiece)) {
                builder.add(headerPiece);
            }

            BitSet indexPieces = prioritizedIndexPieces;
            indexPieces.stream()
                    .filter(moviePieces::get)
                    .filter(relevantChunks::get)
                    .filter(i -> i != headerPiece)
                    .forEach(builder::add);

            int playhead = playheadPiece;
            moviePieces.stream()
                    .filter(i -> i >= playhead)
                    .filter(relevantChunks::get)
                    .filter(i -> i != headerPiece && !indexPieces.get(i))
                    .forEach(builder::add);

            moviePieces.stream()
                    .filter(i -> i < playhead)
                    .filter(relevantChunks::get)
                    .filter(i -> i != headerPiece && !indexPieces.get(i))
                    .forEach(builder::add);

            // Ancillary torrent files still need to finish eventually, after the selected
            // movie's playback pieces have been requested.
            relevantChunks.stream()
                    .filter(i -> !moviePieces.get(i))
                    .forEach(builder::add);
            return builder.build();
        }

        // Until torrent metadata identifies the movie file, fall back to piece zero.
        if (relevantChunks.get(0)) {
            builder.add(0);
        }

        BitSet indexPieces = prioritizedIndexPieces;
        indexPieces.stream()
                .filter(i -> i > 0 && i < totalPieces && relevantChunks.get(i))
                .forEach(builder::add);

        int playhead = playheadPiece;

        // From the playhead to the end of the file...
        relevantChunks.stream()
                .filter(i -> i >= playhead)
                .filter(i -> shouldKeepPiece(i, indexPieces))
                .forEach(builder::add);

        // ...then everything skipped over, so a seek does not abandon it for good.
        relevantChunks.stream()
                .filter(i -> i < playhead)
                .filter(i -> shouldKeepPiece(i, indexPieces))
                .forEach(builder::add);

        return builder.build();
    }

    private boolean shouldKeepPiece(int pieceIndex, BitSet indexPieces) {
        return pieceIndex != 0 && !indexPieces.get(pieceIndex);
    }
}
