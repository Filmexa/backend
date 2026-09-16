/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TrendingMovieProviderResponse.java                        :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:51:10 by maddou            #+#    #+#             */
/*   Updated: 2026/09/14 12:51:10 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendingMovieProviderResponse {
    Long    id;
    String  title;
    String  release_date;
    String  poster_path;
    String  backdrop_path;
    String  overview;
    List<Integer> genre_ids;
}
