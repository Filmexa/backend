/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TorrentService.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/23 13:20:22 by marouan           #+#    #+#             */
/*   Updated: 2026/09/23 19:31:03 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.service;

import java.util.Optional;

import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;

public interface TorrentService {
    Optional<TorrentResultDto> resolve( String imdbId );
}
