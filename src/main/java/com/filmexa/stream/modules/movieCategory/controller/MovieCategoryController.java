/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategoryController.java                       :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 11:13:00 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/22 12:42:22 by kchaouki         ###   ########.fr       */
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.modules.movieCategory.dto.MovieCategoryRequest;
import com.filmexa.stream.modules.movieCategory.dto.MovieCategoryResponse;
import com.filmexa.stream.modules.movieCategory.entity.MovieCategory;
import com.filmexa.stream.modules.movieCategory.service.MovieCategoryService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RestController
@Validated
@RequestMapping("/api/movie-categories")
@Tag(name = "Movie Category", description = "Endpoints for managing movie categories")
public class MovieCategoryController {
    
    private final MovieCategoryService movieCategoryService;

    public MovieCategoryController(MovieCategoryService movieCategoryService) {
        this.movieCategoryService = movieCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<MovieCategoryResponse>> getAllMovieCategories(
            @RequestParam(defaultValue = "en")
            @Pattern(regexp = "^(en|fr|ar)$", message = "Language must be en, fr, or ar")
            String language
    ) {
        return ResponseEntity.ok(
                movieCategoryService.getAllMovieCategoriesResponse(language)
        );
    }

    @PostMapping
    public ResponseEntity<MovieCategoryResponse> createMovieCategory(
            @Valid @RequestBody MovieCategoryRequest request
    ) {
        MovieCategory created = movieCategoryService.createMovieCategory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieCategoryResponse> updateMovieCategory(
            @PathVariable UUID id,
            @Valid @RequestBody MovieCategoryRequest request
    ) {
        MovieCategory updated = movieCategoryService.updateMovieCategory(id, request);

        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovieCategory(@PathVariable UUID id) {
        movieCategoryService.deleteMovieCategory(id);
        return ResponseEntity.noContent().build();
    }

    private MovieCategoryResponse toResponse(MovieCategory category) {
        MovieCategoryResponse response = new MovieCategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName("en"));
        response.setGenreId(category.getGenreId());
        return response;
    }
}
