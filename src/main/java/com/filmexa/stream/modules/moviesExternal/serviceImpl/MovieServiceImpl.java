/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieServiceImpl.java                              :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:33:45 by maddou            #+#    #+#             */
/*   Updated: 2026/09/17 14:39:01 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.serviceImpl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.filmexa.stream.modules.moviesExternal.service.MovieService;
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.dto.response.MovieResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TrendingMovieProviderResponse;
import com.filmexa.stream.modules.moviesExternal.mapper.MovieGenreMapper;
import com.filmexa.stream.modules.moviesExternal.mapper.MovieMapper;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieProvider movieProvider;
    private final MovieMapper   movieMapper;
    
    @Value("${tmdb.base-image-url}")
    private String imageBaseUrl;
    
    public MovieServiceImpl( MovieProvider movieProvider,
        MovieMapper movieMapper
     ) {
        this.movieProvider = movieProvider;
        this.movieMapper = movieMapper;
    }

    @Override
    public List< TrendingMoviesResponse > getTrendingMovies( String language) {

        List< TrendingMovieProviderResponse > providerMovies = movieProvider.getTrendingMovies( language );
        return providerMovies.stream()
        .limit(10)
        .map(movie -> new TrendingMoviesResponse(
            movie.getId(),
            movie.getTitle(),
            movie.getRelease_date(),
            this.imageBaseUrl + movie.getPoster_path(),
            this.imageBaseUrl + movie.getBackdrop_path(),
            movie.getOverview(),
            movie.getGenre_ids()
                .stream()
                .map(id -> MovieGenreMapper.getName(id, language) )
                .toList()
            ))
        .toList();
    }

    @Override
    public Map<String, List<MovieResponse>> buildHomeMovies( String language) {
        // get top rated
        List< MovieResponse > topRated = movieProvider.getTopRatedMovies( language )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();;
        
        // get movies by genre
        List< MovieResponse > action = movieProvider.getMoviesByGenre( language, 28L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        
        List< MovieResponse > comedy = movieProvider.getMoviesByGenre( language, 35L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        
        List< MovieResponse > horror = movieProvider.getMoviesByGenre( language, 27L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        
        List< MovieResponse > drama = movieProvider.getMoviesByGenre( language, 18L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        
        List< MovieResponse > romance = movieProvider.getMoviesByGenre( language, 10749L )
            .stream()
            .map( movie -> this.movieMapper.toMovieResponse( movie ) )
            .toList();
        return this.generateHomeMoviesData(
            topRated,
            action,
            comedy,
            horror,
            drama,
            romance
        );
    }

    private Map<String, List<MovieResponse>> generateHomeMoviesData(
        List< MovieResponse > topRated,
        List< MovieResponse > action,
        List< MovieResponse > comedy,
        List< MovieResponse > horror,
        List< MovieResponse > drama,
        List< MovieResponse > romance
    ) {
        Map<String, List<MovieResponse>> homeMovies = new LinkedHashMap<>();
        homeMovies.put( "topRated", topRated );
        homeMovies.put( "action", action );
        homeMovies.put( "comedy", comedy );
        homeMovies.put( "horror", horror );
        homeMovies.put( "drama", drama );
        homeMovies.put( "romance", romance );
        return homeMovies;
    }
}
