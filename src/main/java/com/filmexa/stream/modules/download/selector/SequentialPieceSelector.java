package com.filmexa.stream.modules.download.selector;

import java.util.BitSet;
import java.util.stream.IntStream;

import bt.torrent.PieceStatistics;
import bt.torrent.selector.PieceSelector;

public class SequentialPieceSelector implements PieceSelector {

    private int totalPieces;
    private boolean isMp4;

    public SequentialPieceSelector(boolean isMp4) {
        this.isMp4 = isMp4;
    }

    @Override
    public void initSelector(int totalPieces) {
        this.totalPieces = totalPieces;
    }

    @Override
    public IntStream getNextPieces(BitSet relevantChunks, PieceStatistics pieceStatistics) {
        if (relevantChunks.isEmpty()) {
            return IntStream.empty();
        }

        IntStream.Builder builder = IntStream.builder();

        if (relevantChunks.get(0)) {
            builder.add(0);
        }

        int lastPiece = totalPieces - 1;
        if (isMp4 && totalPieces > 1 && relevantChunks.get(lastPiece)) {
            builder.add(lastPiece);
        }
        relevantChunks.stream()
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
