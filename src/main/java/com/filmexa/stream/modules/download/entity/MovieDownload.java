/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieDownload.java                                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 15:50:00 by user              #+#    #+#             */
/*   Updated: 2026/09/22 21:42:09 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.download.entity;

import java.time.LocalDateTime;

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
    private Long movieId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String magnetUrl;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DownloadStatus status = DownloadStatus.PENDING;

    private long totalBytes = 0;
    private long downloadedBytes = 0;

    private String fileName;
    private String containerFormat; // "mp4", "mkv", "webm"
    private String storagePath;

    @Column(nullable = false)
    private boolean isReadyToStream = false;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastWatchedAt; // Used for the 30-day cleanup rule
}
