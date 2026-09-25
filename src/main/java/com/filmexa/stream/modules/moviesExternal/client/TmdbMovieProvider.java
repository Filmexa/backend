/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TmdbMovieProvider.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/13 10:29:13 by maddou            #+#    #+#             */
/*   Updated: 2026/09/25 19:12:46 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.client;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesDetailsResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesDetailsPageableResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.request.TmdbMovieDiscoverRequest;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.trailer.TrailerData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.trailer.Videos;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TmdbMoviesPageableResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieData;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieDetailsProviderData;
import com.filmexa.stream.common.exception.NotFoundException;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MovieProvederData;
import com.filmexa.stream.common.exception.ExternalServiceException;

import java.util.List;

@Component
public class TmdbMovieProvider implements MovieProvider {

    private final RestClient restClient;

    public TmdbMovieProvider( @Qualifier("tmdbRestClient")  RestClient restClient ) {
        this.restClient = restClient;
    }

    @Override
    public List< MovieDetailsProviderResponse > getTrendingMovies( String language ){
        try{
            MoviesDetailsResponse response =  this.restClient
                .get()
                .uri("/trending/movie/week?language={language}", language)
                .retrieve()
                .body( MoviesDetailsResponse.class );
            if ( response == null ) {
                return List.of();
            }
            return response.getResults();
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }

    @Override
    public List< MoviesProviderData > getTopRatedMovies( String language ){
        try{
            MovieData response =  this.restClient
                .get()
                .uri("/movie/top_rated?language={language}", language)
                .retrieve()
                .body( MovieData.class );
            if ( response == null ) {
                return List.of();
            }
            return response.getResults();
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }

    @Override
    public TmdbMoviesPageableResponse  getTopRatedMovies( String language, int page ){
        try {
            TmdbMoviesPageableResponse response =  this.restClient
                .get()
                .uri("/movie/top_rated?language={language}&page={page}", language, page )
                .retrieve()
                .body( TmdbMoviesPageableResponse.class );
            if ( response == null ) {
                throw new ExternalServiceException(
                    "External service is unavailable"
                );
            }
            return response;
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }

    @Override
    public List< MoviesProviderData > getMoviesByGenre( String language, Long id ){
        try {
            MovieData response =  this.restClient
                .get()
                .uri("/discover/movie?language={language}" +
                "&with_genres={id}&sort_by=popularity.desc" +
                "&vote_average.gte=7" +
                "&vote_count.gte=500&page=1", language, id )
                .retrieve()
                .body( MovieData.class );
            if ( response == null ) {
                return List.of();
            }
            return response.getResults();
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }
    
    @Override
    public TmdbMoviesPageableResponse getMoviesByGenre( String language, Integer id, int page ){
        try {
            TmdbMoviesPageableResponse response =  this.restClient
                .get()
                .uri("/discover/movie?language={language}" +
                "&with_genres={id}" +
                "&page={page}", language, id, page )
                .retrieve()
                .body( TmdbMoviesPageableResponse.class );
            if ( response == null ) {
                throw new NotFoundException(
                    "Genre does not exist"
                );
            }
            return response;
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }

    @Override
    public TmdbMoviesPageableResponse searchMovie( String language, String query, int page ){
        try {
            TmdbMoviesPageableResponse response =  this.restClient
                .get()
                .uri("/search/movie?language={language}" +
                "&query={query}" +
                "&page={page}", language, query, page )
                .retrieve()
                .body( TmdbMoviesPageableResponse.class );
                if ( response == null ) {
                    throw new ExternalServiceException(
                        "External service is unavailable"
                    );
                }
            return response;
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }
    
    @Override
    public /*List<MoviesProviderData>*/List<MovieDetailsProviderData> searchMovieByQuery( String language, String query, Integer year, int page ){
        try {
            MoviesDetailsPageableResponse response =  this.restClient
            .get()
            .uri("/search/movie?language={language}" +
                "&query={query}" +
                "&page={page}" +
                "&year={year}", language, query, page, year )
                .retrieve()
                .body( MoviesDetailsPageableResponse.class );
            if ( response == null ) {
                return List.of();
            }
            return response.getResults();
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }

    @Override
    public TmdbMoviesPageableResponse discoverMovies(
            TmdbMovieDiscoverRequest request
    ) {
        try {
            TmdbMoviesPageableResponse response = this.restClient.get()
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
            if ( response == null ) {
                throw new ExternalServiceException(
                    "External service is unavailable"
                );
            }
            return response;
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }
    
    @Override
    public MovieProvederData getMovieById( String language, Integer id ) {
        try {
            MovieProvederData movieData = this.restClient
                .get()
                .uri("/movie/{id}?append_to_response=credits&language={language}", 
                    id, 
                    language )
                .retrieve()
                .onStatus(
                    status -> status.value() == 404,
                    (request, response) -> {
                        throw new NotFoundException("Movie does not exist");
                    }
                )
                .body( MovieProvederData.class );
            if ( movieData == null ) {
                throw new NotFoundException(
                    "Movie deos not exist"
                );
            }
            return movieData;
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }

    @Override 
    public List< TrailerData> getTraierMovie( Integer id ) {
        try {
            Videos data = this.restClient
                .get()
                .uri("/movie/{id}/videos", 
                    id)
                .retrieve()
                .onStatus(
                    status -> status.value() == 404,
                    (request, response) -> {
                        throw new NotFoundException("Movie does not exist");
                    }
                )
                .body( Videos.class );
            if ( data == null ) {
                return List.of();
            }
            return data.getResults();
        }
        catch (RestClientException e) {
            throw new ExternalServiceException(
                    "External service is unavailable",
                    e
            );
        }
    }
}
