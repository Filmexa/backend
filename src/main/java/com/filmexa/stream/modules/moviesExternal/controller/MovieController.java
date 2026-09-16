/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieController.java                               :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/13 10:27:27 by maddou            #+#    #+#             */
/*   Updated: 2026/09/16 15:09:32 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.filmexa.stream.modules.moviesExternal.service.MovieService;
import com.filmexa.stream.modules.moviesExternal.dto.request.MovieQuery;
import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.response.MovieResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@Tag(
    name = "Movies",
    description = "Movie catalogue"
)
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController( MovieService movieService ) {
        this.movieService = movieService;
    }

    @GetMapping("/trending/week")
    public List< TrendingMoviesResponse >getTrendingMovies( @Valid @ModelAttribute MovieQuery query ) {
        return this.movieService.getTrendingMovies( query.getLanguage() );
    }

    @GetMapping("/home")
    public Map<String, List<MovieResponse>> getHomeData( @Valid @ModelAttribute MovieQuery query ) {
        return this.movieService.buildHomeMovies( query.getLanguage() );
    }
}
