/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   Movie.java                                         :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/21 11:27:30 by marouan           #+#    #+#             */
/*   Updated: 2026/09/21 11:30:15 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.dto.yts;

import java.util.List;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    private List<YtsTorrentDto> torrents;
}
