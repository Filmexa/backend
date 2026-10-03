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

        // A small movie fits entirely in the header window, so every piece is sequential.
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), order);
    }

    @Test
    void seekingForwardAsksForThatPartFirst_thenFillsInWhatWasSkipped() {
        int totalPieces = 100;
        SequentialPieceSelector selector = new SequentialPieceSelector(false);
        selector.initSelector(totalPieces);

        BitSet availablePieces = new BitSet(totalPieces);
        availablePieces.set(0, totalPieces);

        // Three quarters in: the viewer jumped towards the end of the film.
        assertEquals(75, selector.seekToFraction(0.75));

        List<Integer> order = selector.getNextPieces(availablePieces, null)
                .boxed()
                .toList();

        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
                order.subList(0, 16));
        assertEquals(75, order.get(16));
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

    @Test
    void movieFractionsAreMappedInsideItsMultiFileTorrentRange() {
        SequentialPieceSelector selector = new SequentialPieceSelector(false);
        selector.initSelector(20);
        selector.configureMovieRange(4, 13, true);

        assertEquals(4, selector.pieceAtFraction(0));
        assertEquals(9, selector.seekToFraction(0.5));
        assertEquals(13, selector.pieceAtFraction(1));

        BitSet availablePieces = new BitSet(20);
        availablePieces.set(0, 20);
        List<Integer> order = selector.getNextPieces(availablePieces, null).boxed().toList();

        assertEquals(List.of(4, 5, 6, 7, 8, 9, 10, 11, 12, 13), order.subList(0, 10));
    }

    @Test
    void largeMp4PrioritisesBothHeaderAndTailWindows() {
        SequentialPieceSelector selector = new SequentialPieceSelector(false);
        selector.initSelector(200);
        selector.configureMovieRange(10, 189, "feature.MOV");

        BitSet available = new BitSet(200);
        available.set(0, 200);
        List<Integer> order = selector.getNextPieces(available, null).boxed().toList();

        assertEquals(10, order.get(0));
        assertEquals(25, order.get(15));
        assertEquals(174, order.get(16));
        assertEquals(189, order.get(31));
    }

    @Test
    void webmPrioritisesOpeningHeaderAndEndingCues() {
        SequentialPieceSelector selector = new SequentialPieceSelector(false);
        selector.initSelector(200);
        selector.configureMovieRange(10, 189, "feature.webm");
        selector.seekToFraction(0.75);

        BitSet available = new BitSet(200);
        available.set(0, 200);
        List<Integer> order = selector.getNextPieces(available, null).boxed().toList();

        assertEquals(10, order.get(0));
        assertEquals(25, order.get(15));
        assertEquals(174, order.get(16));
        assertEquals(189, order.get(31));
        assertEquals(145, order.get(32));
    }
}
