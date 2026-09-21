/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieController.java                               :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/13 10:27:27 by maddou            #+#    #+#             */
/*   Updated: 2026/09/21 15:54:59 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.filmexa.stream.modules.moviesExternal.service.MovieService;
import com.filmexa.stream.modules.moviesExternal.dto.request.MovieQuery;
import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.MovieResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.MovieDetailsResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.MoviePageResponse;
import com.filmexa.stream.modules.torrent.dto.TorrentResultDto;

// # you must deleted
import com.filmexa.stream.modules.torrent.client.TorrentClient;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@Tag(
    name = "Movies",
    description = "Movie catalogue"
)
@RestController
@Validated
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;
    
    // # you must deleted
    private final TorrentClient torrentClient;
    
    public MovieController( MovieService movieService, @Qualifier("pirateBay") TorrentClient torrentClient) {
        this.movieService = movieService;
        this.torrentClient = torrentClient;
    }

    @GetMapping("/trending/week")
    public List< TrendingMoviesResponse >getTrendingMovies( @Valid @ModelAttribute MovieQuery query ) {
        return this.movieService.getTrendingMovies( query.getLanguage() );
    }

    @GetMapping("/home")
    public Map<String, List<MovieResponse>> getHomeData( @Valid @ModelAttribute MovieQuery query ) {
        return this.movieService.buildHomeMovies( query.getLanguage() );
    }

    @GetMapping("/test")
    public  List<TorrentResultDto> getHomeData( ) {
        return this.torrentClient.search( "tt1375666" );
        // return ;
    }
    
    @GetMapping("/genre/{id}")
    public MoviePageResponse getMovieByGenre( @PathVariable 
        @Positive(message = "ID must be greater than 0") 
        @Max(100000)
        Integer id,
        
        @Valid @ModelAttribute MovieQuery query,
        Pageable pageable
    ) {
        return this.movieService.getMoviesByGenre( query.getLanguage(), id, pageable );
    }

    @GetMapping("/{id}")
    public MovieDetailsResponse getMovieById( @PathVariable 
        @Positive(message = "ID must be greater than 0") 
        @Max(Integer.MAX_VALUE)
        Integer id,
        
        @Valid @ModelAttribute MovieQuery query,
        Pageable pageable
    ) {
        return this.movieService.getMovieById( query.getLanguage(), id );
    }
}
