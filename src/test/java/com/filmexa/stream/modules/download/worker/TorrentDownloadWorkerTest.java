package com.filmexa.stream.modules.download.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.BitSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import bt.data.DataDescriptor;
import bt.data.LocalBitfield;
import bt.metainfo.Torrent;
import bt.metainfo.TorrentFile;
import bt.metainfo.TorrentId;
import bt.runtime.BtRuntime;
import bt.torrent.TorrentDescriptor;
import bt.torrent.TorrentRegistry;
import com.filmexa.stream.modules.download.config.TorrentRuntimePool;
import com.filmexa.stream.modules.download.repo.MovieDownloadRepository;
import com.filmexa.stream.modules.download.selector.SequentialPieceSelector;

class TorrentDownloadWorkerTest {

    @Test
    void configuresMp4MoviePiecesAndIndexPriorityFromTorrentMetadata() {
        MovieDownloadRepository downloads = mock(MovieDownloadRepository.class);
        TorrentRuntimePool runtimePool = mock(TorrentRuntimePool.class);
        TorrentDownloadWorker worker = new TorrentDownloadWorker(downloads, runtimePool, task -> { });

        BtRuntime runtime = mock(BtRuntime.class);
        TorrentRegistry registry = mock(TorrentRegistry.class);
        Torrent torrent = mock(Torrent.class);
        TorrentFile ancillary = mock(TorrentFile.class);
        TorrentFile video = mock(TorrentFile.class);
        TorrentDescriptor descriptor = mock(TorrentDescriptor.class);
        DataDescriptor data = mock(DataDescriptor.class);
        LocalBitfield bitfield = mock(LocalBitfield.class);
        TorrentId torrentId = mock(TorrentId.class);

        when(runtime.service(TorrentRegistry.class)).thenReturn(registry);
        when(registry.getTorrent(torrentId)).thenReturn(Optional.of(torrent));
        when(registry.getDescriptor(torrentId)).thenReturn(Optional.of(descriptor));
        when(torrent.getFiles()).thenReturn(List.of(ancillary, video));
        when(ancillary.getPathElements()).thenReturn(List.of("readme.txt"));
        when(ancillary.getSize()).thenReturn(3_000_000L);
        when(video.getPathElements()).thenReturn(List.of("Chainsaw.Man.The.Movie.mp4"));
        when(video.getSize()).thenReturn(100_000_000L);
        when(torrent.getChunkSize()).thenReturn(1_000_000L);
        when(descriptor.getDataDescriptor()).thenReturn(data);
        when(data.getBitfield()).thenReturn(bitfield);
        when(bitfield.getPiecesTotal()).thenReturn(103);

        BitSet mp4Pieces = new BitSet();
        mp4Pieces.set(3, 103);
        when(data.getAllPiecesForFiles(Set.of(video))).thenReturn(mp4Pieces);
        BitSet completePieces = new BitSet(103);
        completePieces.set(3);
        completePieces.set(101, 103);
        when(bitfield.isComplete(anyInt()))
                .thenAnswer(invocation -> completePieces.get(invocation.getArgument(0)));

        SequentialPieceSelector selector = new SequentialPieceSelector();
        selector.initSelector(103);
        assertThat(worker.configureVideoFileLayout(runtime, torrentId, selector, 1218925L)).isTrue();

        // Do not make ffprobe wait for the entire 32 MiB prefetch window.
        assertThat(worker.isInitialProbeDataDownloaded(1218925L)).contains(true);
        completePieces.clear(101, 103);
        assertThat(worker.isInitialProbeDataDownloaded(1218925L)).contains(false);
        completePieces.set(101, 103);
        assertThat(worker.isInitialProbeDataDownloaded(1218925L)).contains(true);

        BitSet relevantPieces = new BitSet(103);
        relevantPieces.set(0, 103);
        List<Integer> order = selector.getNextPieces(relevantPieces, null).boxed().toList();
        List<Integer> prioritizedTail = IntStream.rangeClosed(69, 102).boxed().toList();
        assertThat(order.get(0)).isEqualTo(3);
        assertThat(order.subList(1, 35)).containsExactlyElementsOf(prioritizedTail);
        assertThat(order.get(35)).isEqualTo(4);
        assertThat(order.subList(order.size() - 3, order.size())).containsExactly(0, 1, 2);
    }

    @Test
    void readinessChecksUseTheSelectedFilesOffsetInsteadOfTorrentPieceZero() {
        MovieDownloadRepository downloads = mock(MovieDownloadRepository.class);
        TorrentRuntimePool runtimePool = mock(TorrentRuntimePool.class);
        TorrentDownloadWorker worker = new TorrentDownloadWorker(downloads, runtimePool, task -> { });

        BtRuntime runtime = mock(BtRuntime.class);
        TorrentRegistry registry = mock(TorrentRegistry.class);
        Torrent torrent = mock(Torrent.class);
        TorrentFile ancillary = mock(TorrentFile.class);
        TorrentFile video = mock(TorrentFile.class);
        TorrentDescriptor descriptor = mock(TorrentDescriptor.class);
        DataDescriptor data = mock(DataDescriptor.class);
        LocalBitfield bitfield = mock(LocalBitfield.class);
        TorrentId torrentId = mock(TorrentId.class);

        when(runtime.service(TorrentRegistry.class)).thenReturn(registry);
        when(registry.getTorrent(torrentId)).thenReturn(Optional.of(torrent));
        when(registry.getDescriptor(torrentId)).thenReturn(Optional.of(descriptor));
        when(torrent.getFiles()).thenReturn(List.of(ancillary, video));
        when(ancillary.getPathElements()).thenReturn(List.of("readme.txt"));
        when(ancillary.getSize()).thenReturn(100L);
        when(video.getPathElements()).thenReturn(List.of("Oak.Street.mkv"));
        when(video.getSize()).thenReturn(1_000L);
        when(torrent.getChunkSize()).thenReturn(100L);
        when(descriptor.getDataDescriptor()).thenReturn(data);
        when(data.getBitfield()).thenReturn(bitfield);
        when(bitfield.getPiecesTotal()).thenReturn(11);

        BitSet mkvPieces = new BitSet();
        mkvPieces.set(1, 11);
        when(data.getAllPiecesForFiles(Set.of(video))).thenReturn(mkvPieces);

        AtomicBoolean movieHeaderComplete = new AtomicBoolean(false);
        when(bitfield.isComplete(0)).thenReturn(true);
        when(bitfield.isComplete(1)).thenAnswer(invocation -> movieHeaderComplete.get());

        SequentialPieceSelector selector = new SequentialPieceSelector();
        selector.initSelector(11);
        assertThat(worker.configureVideoFileLayout(runtime, torrentId, selector, 1101383L)).isTrue();

        // Piece zero belongs to readme.txt, so it cannot make the movie appear playable.
        assertThat(worker.isRangeDownloaded(1101383L, 0.0, 0.001)).contains(false);

        movieHeaderComplete.set(true);
        assertThat(worker.isRangeDownloaded(1101383L, 0.0, 0.001)).contains(true);

        BitSet relevantPieces = new BitSet(11);
        relevantPieces.set(0, 11);
        assertThat(selector.getNextPieces(relevantPieces, null).boxed().toList())
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0);
    }
}
