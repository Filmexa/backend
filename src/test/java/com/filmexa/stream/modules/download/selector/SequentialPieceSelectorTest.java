package com.filmexa.stream.modules.download.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class SequentialPieceSelectorTest {

    @Test
    void testMp4SequentialOrder_piece0First_lastPieceSecond() {
        int totalPieces = 10;
        SequentialPieceSelector selector = new SequentialPieceSelector(true);
        selector.initSelector(totalPieces);

        BitSet availablePieces = new BitSet(totalPieces);
        availablePieces.set(0, totalPieces); // Pieces 0 to 9 available

        List<Integer> order = selector.getNextPieces(availablePieces, null)
                .boxed()
                .toList();

        // 1. Piece 0 must be FIRST
        assertEquals(0, order.get(0));

        // 2. Last piece (9) must be SECOND (for MP4 moov atom)
        assertEquals(9, order.get(1));

        // 3. Middle pieces in sequential order (1, 2, 3, 4, 5, 6, 7, 8)
        assertEquals(List.of(0, 9, 1, 2, 3, 4, 5, 6, 7, 8), order);
    }

    @Test
    void seekingForwardAsksForThatPartFirst_thenFillsInWhatWasSkipped() {
        int totalPieces = 10;
        SequentialPieceSelector selector = new SequentialPieceSelector(false);
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
        SequentialPieceSelector selector = new SequentialPieceSelector(false);
        selector.initSelector(10);

        assertEquals(9, selector.seekToFraction(1.5));
        assertEquals(0, selector.seekToFraction(-0.2));
    }

    @Test
    void seekBeforeTheMetadataArrivesIsIgnored() {
        SequentialPieceSelector selector = new SequentialPieceSelector(false);

        // initSelector has not run yet, so the piece count is unknown.
        assertEquals(0, selector.seekToFraction(0.75));
        assertEquals(0, selector.getPlayheadPiece());
    }
}
