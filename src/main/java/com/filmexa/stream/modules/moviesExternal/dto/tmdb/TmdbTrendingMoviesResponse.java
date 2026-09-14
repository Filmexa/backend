/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TmdbTrendingMoviesResponse.java                    :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 19:06:57 by maddou            #+#    #+#             */
/*   Updated: 2026/09/14 19:09:03 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.dto.tmdb;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TrendingMovieProviderResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TmdbTrendingMoviesResponse {
    private List<TrendingMovieProviderResponse> results;
}
