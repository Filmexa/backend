/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieService.java                                  :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:33:09 by maddou            #+#    #+#             */
/*   Updated: 2026/09/16 13:25:51 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.service;

import java.util.List;
import java.util.Map;

import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.MovieResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;

public interface MovieService {
    List< TrendingMoviesResponse > getTrendingMovies( String language ); 
    Map<String, List<MovieResponse>> buildHomeMovies( String language );
    // /*List< TrendingMoviesResponse > */void getHomeData( String language ); 
}
