/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategoryController.java                       :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 11:13:00 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/14 13:07:00 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.movieCategory.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.common.utils.ErrorResponse;
import com.filmexa.stream.modules.movieCategory.dto.MovieCategoryRequest;
import com.filmexa.stream.modules.movieCategory.dto.MovieCategoryResponse;
import com.filmexa.stream.modules.movieCategory.entity.MovieCategory;
import com.filmexa.stream.modules.movieCategory.service.MovieCategoryService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/movie-categories")
@Tag(name = "Movie Category", description = "Endpoints for managing movie categories")
public class MovieCategoryController {
    
    private final MovieCategoryService movieCategoryService;

    public MovieCategoryController(MovieCategoryService movieCategoryService) {
        this.movieCategoryService = movieCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<MovieCategoryResponse>> getAllMovieCategories() {
        return ResponseEntity.ok(
                movieCategoryService.getAllMovieCategoriesResponse()
        );
    }

    @PostMapping
    public ResponseEntity<?> createMovieCategory(
            @Valid @RequestBody MovieCategoryRequest request
    ) {
        try {
            MovieCategory created = movieCategoryService.createMovieCategory(request);
    
            MovieCategoryResponse response = new MovieCategoryResponse();
            response.setId(created.getId());
            response.setName(created.getName());
            response.setGenreId(created.getGenreId());
    
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieCategory> updateMovieCategory(@Valid @RequestBody MovieCategory request) {
        return ResponseEntity.ok(
                movieCategoryService.updateMovieCategory(request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovieCategory(@PathVariable UUID id) {
        try {
            movieCategoryService.deleteMovieCategory(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }
}
