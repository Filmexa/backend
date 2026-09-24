/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   YtsData.java                                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/21 11:26:47 by marouan           #+#    #+#             */
/*   Updated: 2026/09/21 11:31:00 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.torrent.dto.yts;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YtsData {
    private Movie movie;
}
