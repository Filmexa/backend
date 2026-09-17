/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TmdbMovieProvider.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/13 10:29:13 by maddou            #+#    #+#             */
/*   Updated: 2026/09/17 11:27:46 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.client;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TmdbTrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TrendingMovieProviderResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TmdbMoviesPageableResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieData;

import java.util.List;

@Component
public class TmdbMovieProvider implements MovieProvider {

    private final RestClient restClient;

    public TmdbMovieProvider( @Qualifier("tmdbRestClient")  RestClient restClient ) {
        this.restClient = restClient;
    }

    @Override
    public List< TrendingMovieProviderResponse > getTrendingMovies( String language ){
        TmdbTrendingMoviesResponse response =  this.restClient
            .get()
            .uri("/trending/movie/week?language={language}", language)
            .retrieve()
            .body( TmdbTrendingMoviesResponse.class );
        return response.getResults();
    }

    @Override
    public List< MoviesProviderData > getTopRatedMovies( String language ){
        MovieData response =  this.restClient
            .get()
            .uri("/movie/top_rated?language={language}", language)
            .retrieve()
            .body( MovieData.class );
        return response.getResults();
    }

    //with_genres={genreId} // Filter by genre 
    // &sort_by=popularity.desc // Most popular first 
    // &vote_average.gte=7 // Rating >= 7/10 
    // &vote_count.gte=500 // At least 500 votes
    @Override
    public List< MoviesProviderData > getMoviesByGenre( String language, Long id ){
        MovieData response =  this.restClient
            .get()
            .uri("/discover/movie?language={language}" +
            "&with_genres={id}&sort_by=popularity.desc" +
            "&vote_average.gte=7" +
            "&vote_count.gte=500&page=1", language, id )
            .retrieve()
            .body( MovieData.class );
        return response.getResults();
    }
    
    @Override
    public TmdbMoviesPageableResponse getMoviesByGenre( String language, Integer id, int page ){
        TmdbMoviesPageableResponse response =  this.restClient
            .get()
            .uri("/discover/movie?language={language}" +
            "&with_genres={id}&sort_by=popularity.desc" +
            "&page={page}", language, id, page )
            .retrieve()
            .body( TmdbMoviesPageableResponse.class );
        return response;
    }
}
