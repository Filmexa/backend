/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TmdbMovieProvider.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/13 10:29:13 by maddou            #+#    #+#             */
/*   Updated: 2026/09/20 00:32:04 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.client;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesDetailsResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesDetailsPageableResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.request.TmdbMovieDiscoverRequest;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TmdbMoviesPageableResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderData;

import java.util.List;

@Component
public class TmdbMovieProvider implements MovieProvider {

    private final RestClient restClient;

    public TmdbMovieProvider( @Qualifier("tmdbRestClient")  RestClient restClient ) {
        this.restClient = restClient;
    }

    @Override
    public List< MovieDetailsProviderResponse > getTrendingMovies( String language ){
        MoviesDetailsResponse response =  this.restClient
            .get()
            .uri("/trending/movie/week?language={language}", language)
            .retrieve()
            .body( MoviesDetailsResponse.class );
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
            "&with_genres={id}" +
            "&page={page}", language, id, page )
            .retrieve()
            .body( TmdbMoviesPageableResponse.class );
        return response;
    }

    @Override
    public TmdbMoviesPageableResponse searchMovie( String language, String query, int page ){
        TmdbMoviesPageableResponse response =  this.restClient
            .get()
            .uri("/search/movie?language={language}" +
            "&query={query}" +
            "&page={page}", language, query, page )
            .retrieve()
            .body( TmdbMoviesPageableResponse.class );
        return response;
    }
    
    @Override
    public /*List<MoviesProviderData>*/List<MovieDetailsProviderData> searchMovieByQuery( String language, String query, Integer year, int page ){
        /*SearchByQuery*/MoviesDetailsPageableResponse response =  this.restClient
        .get()
        .uri("/search/movie?language={language}" +
            "&query={query}" +
            "&page={page}" +
            "&year={year}", language, query, page, year )
            .retrieve()
            .body( MoviesDetailsPageableResponse.class );
        
        return response.getResults();
    }

    @Override
public TmdbMoviesPageableResponse discoverMovies(
        TmdbMovieDiscoverRequest request
) {
    return this.restClient.get()
            .uri(
                "/discover/movie?language={language}" +
                "&with_genres={genreId}" +
                "&year={year}" +
                "&vote_average.gte={minRating}" +
                "&sort_by={sortBy}" +
                "&page={page}",
                request.getLanguage(),
                request.getGenreId(),
                request.getYear(),
                request.getMinRating(),
                request.getSortBy(),
                request.getPage()
            )
            .retrieve()
            .body( TmdbMoviesPageableResponse.class );
}
}
