package com.filmexa.stream.modules.download.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class SequentialPieceSelectorTest {

    @Test
    void headerAndConfiguredMp4IndexAreRequestedBeforeTheRest() {
        int totalPieces = 10;
        SequentialPieceSelector selector = new SequentialPieceSelector();
        BitSet indexPieces = new BitSet(totalPieces);
        indexPieces.set(9);
        selector.setIndexPieces(indexPieces);
        selector.initSelector(totalPieces);

        BitSet availablePieces = new BitSet(totalPieces);
        availablePieces.set(0, totalPieces); // Pieces 0 to 9 available

        List<Integer> order = selector.getNextPieces(availablePieces, null)
                .boxed()
                .toList();

        // 1. Piece 0 must be FIRST
        assertEquals(0, order.get(0));

        // 2. The MP4's last piece (9) must be SECOND (for its moov atom).
        assertEquals(9, order.get(1));

        // 3. Middle pieces in sequential order (1, 2, 3, 4, 5, 6, 7, 8)
        assertEquals(List.of(0, 9, 1, 2, 3, 4, 5, 6, 7, 8), order);
    }

    @Test
    void seekingForwardAsksForThatPartFirst_thenFillsInWhatWasSkipped() {
        int totalPieces = 10;
        SequentialPieceSelector selector = new SequentialPieceSelector();
        selector.initSelector(totalPieces);

        BitSet availablePieces = new BitSet(totalPieces);
        availablePieces.set(0, totalPieces);

        // Three quarters in: the viewer jumped towards the end of the film.
        assertEquals(7, selector.seekToFraction(0.75));

        List<Integer> order = selector.getNextPieces(availablePieces, null)
                .boxed()
                .toList();

        // The header still comes first, then the seeked-to part, then the skipped middle.
        assertEquals(List.of(0, 7, 8, 9, 1, 2, 3, 4, 5, 6), order);
    }

    @Test
    void seekFractionIsClampedToTheFile() {
        SequentialPieceSelector selector = new SequentialPieceSelector();
        selector.initSelector(10);

        assertEquals(9, selector.seekToFraction(1.5));
        assertEquals(0, selector.seekToFraction(-0.2));
    }

    @Test
    void seekBeforeTheMetadataArrivesIsIgnored() {
        SequentialPieceSelector selector = new SequentialPieceSelector();

        // initSelector has not run yet, so the piece count is unknown.
        assertEquals(0, selector.seekToFraction(0.75));
        assertEquals(0, selector.getPlayheadPiece());
    }

    @Test
    void indexPieceStaysPrioritizedAfterASeek() {
        SequentialPieceSelector selector = new SequentialPieceSelector();
        selector.initSelector(10);
        BitSet indexPieces = new BitSet(10);
        indexPieces.set(9);
        selector.setIndexPieces(indexPieces);
        selector.seekToFraction(0.75);

        BitSet availablePieces = new BitSet(10);
        availablePieces.set(0, 10);

        assertEquals(List.of(0, 9, 7, 8, 1, 2, 3, 4, 5, 6),
                selector.getNextPieces(availablePieces, null).boxed().toList());
    }

    @Test
    void movieHeaderSeekAndIndexUseTheSelectedFilesGlobalTorrentPieces() {
        SequentialPieceSelector selector = new SequentialPieceSelector();
        selector.initSelector(10);

        BitSet moviePieces = new BitSet(10);
        moviePieces.set(3, 9); // The video file is stored after three ancillary pieces.
        selector.setVideoPieces(moviePieces);

        BitSet indexPieces = new BitSet(10);
        indexPieces.set(7, 9);
        selector.setIndexPieces(indexPieces);

        assertEquals(7, selector.seekToFraction(0.75));

        BitSet relevantPieces = new BitSet(10);
        relevantPieces.set(0, 10);
        assertEquals(List.of(3, 7, 8, 4, 5, 6, 0, 1, 2, 9),
                selector.getNextPieces(relevantPieces, null).boxed().toList());
    }
}
