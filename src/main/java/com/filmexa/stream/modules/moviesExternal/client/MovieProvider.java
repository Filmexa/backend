/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieProvider.java                                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:39:46 by maddou            #+#    #+#             */
/*   Updated: 2026/09/17 23:29:23 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.client;

import java.util.List;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TrendingMovieProviderResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieProvederData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TmdbMoviesPageableResponse;

public interface MovieProvider {
    List< TrendingMovieProviderResponse > getTrendingMovies( String language );
    List< MoviesProviderData > getTopRatedMovies( String language );
    List< MoviesProviderData > getMoviesByGenre( String language, Long id );
    TmdbMoviesPageableResponse getMoviesByGenre( String language, Integer id, int page );
    TmdbMoviesPageableResponse searchMovie( String language, String query, int page );
    MovieProvederData getMovieById( String language, Integer id );
}
