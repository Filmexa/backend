/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieService.java                                  :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:33:09 by maddou            #+#    #+#             */
/*   Updated: 2026/09/19 20:42:57 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.MovieResponse;
import com.filmexa.stream.modules.moviesExternal.dto.request.MovieSearchQuery;
import com.filmexa.stream.modules.moviesExternal.dto.response.MoviePageResponse;

public interface MovieService {
    List< TrendingMoviesResponse > getTrendingMovies( String language ); 
    Map<String, List<MovieResponse>> buildHomeMovies( String language );
    List<MovieResponse> getMoviesByGenre( String language, Long id );
    MoviePageResponse getMoviesByGenre( String language, Integer id, Pageable page );
    MoviePageResponse searchMovie( MovieSearchQuery query, Pageable page );
}
