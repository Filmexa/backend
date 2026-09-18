package com.filmexa.stream.modules.moviesExternal.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.modules.moviesExternal.dto.request.MovieSearchQuery;
import com.filmexa.stream.modules.moviesExternal.dto.response.MoviePageResponse;
import com.filmexa.stream.modules.moviesExternal.service.MovieService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/search/movie")
public class SerachMovieController {

    private final MovieService movieService;

    public SerachMovieController( MovieService movieService ) {
        this.movieService = movieService;
    }

    @GetMapping
    public MoviePageResponse  searchMoviesByTitle( 
        @Valid @ModelAttribute MovieSearchQuery query,
        Pageable pageable
    ) {
        return this.movieService.searchMovie( query.getLanguage(), query.getQuery(), pageable );
    }
}
