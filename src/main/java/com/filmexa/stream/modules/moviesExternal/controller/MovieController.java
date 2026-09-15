/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieController.java                               :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/13 10:27:27 by maddou            #+#    #+#             */
/*   Updated: 2026/09/15 19:48:08 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.moviesExternal.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;

import com.filmexa.stream.modules.moviesExternal.service.MovieService;
import com.filmexa.stream.modules.moviesExternal.dto.request.MovieQuery;
import com.filmexa.stream.modules.moviesExternal.dto.response.TrendingMoviesResponse;
import com.filmexa.stream.modules.moviesExternal.dto.tmdb.MoviesProviderData;

import jakarta.validation.Valid;

import java.util.List;

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
    public /*List< TrendingMoviesResponse >*/ List< MoviesProviderData > retrieveHomeData( @Valid @ModelAttribute MovieQuery query ) {
        // return this.movieService.getTrendingMovie();
        return this.movieService.getHomeMovies( query.getLanguage() );
    }
}
