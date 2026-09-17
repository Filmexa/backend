/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieDownload.java                                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: user <user@student.1337.ma>                +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 15:50:00 by user              #+#    #+#             */
/*   Updated: 2026/09/17 15:50:00 by user             ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.download.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.filmexa.stream.common.utils.AbstractEntity;
import com.filmexa.stream.modules.download.enums.DownloadStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Entity
@Table(name = "movie_downloads")
@Data
@EqualsAndHashCode(callSuper = true)
public class MovieDownload extends AbstractEntity {

    @Column(nullable = false, unique = true)
    private UUID movieId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String magnetUrl;

    @Column(nullable = false)
    private String infoHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DownloadStatus status = DownloadStatus.PENDING;

    private long totalBytes = 0;
    private long downloadedBytes = 0;
    private long pieceLength = 0;
    private int totalPieces = 0;

    private String fileName;
    private String containerFormat; // "mp4", "mkv", "webm"
    private String filePath;

    @Column(nullable = false)
    private boolean isReadyToStream = false;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastWatchedAt; // Used for the 30-day cleanup rule
}
