/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   DownloadStatus.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: user <user@student.1337.ma>                +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 15:05:00 by user              #+#    #+#             */
/*   Updated: 2026/09/17 15:05:00 by user             ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.download.enums;

/**
 * Represents the lifecycle states of a movie torrent download in Hypertube.
 */
public enum DownloadStatus {
    /**
     * Download has been requested. Currently resolving magnet metadata and connecting to trackers.
     */
    PENDING,

    /**
     * Connected to peer swarm. Pieces are actively downloading sequentially.
     */
    DOWNLOADING,

    /**
     * The initial buffer threshold (e.g. 10 MB contiguous from piece 0) is verified on disk.
     * The video player can safely begin streaming playback.
     */
    READY_TO_STREAM,

    /**
     * 100% of torrent pieces have been downloaded and verified via SHA-1 hashes.
     * Stored persistently on disk to avoid re-downloading.
     */
    COMPLETED,

    /**
     * Download has been paused by the user or system.
     */
    PAUSED,

    /**
     * An unrecoverable error occurred (e.g. dead torrent, zero peers, disk write error).
     */
    FAILED
}
