/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   YtsTorrentDto.java                                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/21 11:28:14 by marouan           #+#    #+#             */
/*   Updated: 2026/09/22 12:57:46 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.dto.yts;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YtsTorrentDto {
    private String  url;
    private String  hash;
    private String  quality;
    private String  type;
    private String  size;
    private Integer seeds;
    private Integer peers;
    private Long    size_bytes; 
}
