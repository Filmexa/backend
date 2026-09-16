/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   DiscoverMoviesDto.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 11:29:51 by maddou            #+#    #+#             */
/*   Updated: 2026/09/14 15:09:16 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb;

import lombok.Data;
import java.util.List;

@Data
public class DiscoverMoviesDto {
    Long    id;
    String  title;
    String  release_date;
    String  poster_path;
}
