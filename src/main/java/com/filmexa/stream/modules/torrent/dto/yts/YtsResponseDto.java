/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   YtsResponseDto.java                               :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/20 17:41:22 by marouan           #+#    #+#             */
/*   Updated: 2026/09/21 11:18:59 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.dto.yts;

import java.util.List;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

// import com.filmexa.stream.modules.torrent.dto.yts.YtsData;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YtsResponseDto {
    private YtsData data;
}