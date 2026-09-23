/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TrailerData.java                                   :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/23 00:55:12 by maddou            #+#    #+#             */
/*   Updated: 2026/09/23 00:56:39 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb.trailer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor
@NoArgsConstructor
public class TrailerData {
    private Integer size;
    private String type;
    private String  key; 
}
