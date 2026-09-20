/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieProvider.java                                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:39:46 by maddou            #+#    #+#             */
/*   Updated: 2026/09/20 01:10:35 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.client;

import java.util.List;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.request.TmdbMovieDiscoverRequest;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieProvederData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TmdbMoviesPageableResponse;

public interface MovieProvider {
    List< MovieDetailsProviderResponse > getTrendingMovies( String language );
    List< MoviesProviderData > getTopRatedMovies( String language );
    List< MoviesProviderData > getMoviesByGenre( String language, Long id );
    TmdbMoviesPageableResponse getMoviesByGenre( String language, Integer id, int page );
    TmdbMoviesPageableResponse searchMovie( String language, String query, int page );
    /*List< MoviesProviderData >*/List<MovieDetailsProviderData> searchMovieByQuery( String language, String query, Integer year, int page );
    TmdbMoviesPageableResponse  discoverMovies( TmdbMovieDiscoverRequest request );
    MovieProvederData getMovieById( String language, Integer id );
}
