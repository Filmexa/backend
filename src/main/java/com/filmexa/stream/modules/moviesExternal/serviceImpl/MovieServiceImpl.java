/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieServiceImpl.java                              :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 12:33:45 by maddou            #+#    #+#             */
/*   Updated: 2026/09/14 20:57:05 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.serviceImpl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.List;

import com.filmexa.stream.modules.moviesExternal.service.MovieService;
import com.filmexa.stream.modules.movie.dto.MovieResponse;
import com.filmexa.stream.modules.moviesExternal.client.MovieProvider;
import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.TrendingMovieProviderResponse;
// import com.filmexa.stream.modules.moviesExternal.mapper.MovieGenreMapper;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieProvider movieProvider;

    @Value("${tmdb.base-image-url}")
    private String imageBaseUrl;
    
    public MovieServiceImpl( MovieProvider movieProvider ) {
        this.movieProvider = movieProvider;
    }

    @Override
    public List< TrendingMoviesResponse > getTrendingMovie( ) {

        List< TrendingMovieProviderResponse > providerMovies = movieProvider.getTrendingMovies();
        return providerMovies.stream()
        .map(movie -> new TrendingMoviesResponse(
            movie.getId(),
            movie.getTitle(),
            movie.getRelease_date(),
            this.imageBaseUrl + movie.getPoster_path(),
            this.imageBaseUrl + movie.getBackdrop_path(),
            movie.getOverview()
            // movie.genre_ids()
            //     .stream()
            //     .map(String::valueOf)
            //     .toList()
            // movie.getGenre_ids()
            //     .stream()
            //     .map( MovieGenreMapper::getName )
            //     .toList()
            ))
        .toList();
    }
}
