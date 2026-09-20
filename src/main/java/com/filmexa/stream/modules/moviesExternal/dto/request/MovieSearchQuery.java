package com.filmexa.stream.modules.moviesExternal.dto.request;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.filmexa.stream.modules.moviesExternal.enums.MovieSort;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieSearchQuery {

    @Pattern(
        regexp = "^(en|fr|ar)$",
        message = "Language must be en, fr, or ar"
    )
    private String language = "en";

    @Size(
        min = 2,
        max = 100,
        message = "Query must be between 2 and 100 characters"
    )
    private String query;

    private Integer genreId;

    private Integer year;

    @DecimalMin (
        value = "0.0",
        message = "Minimum rating must be at least 0"
    )
    @DecimalMax (
        value = "10.0",
        message = "Minimum rating must be at most 10"
    )
    private Double minRating;

    @Schema(
        description = "Movie sorting",
        allowableValues = {"POPULARITY", "RATING", "RELEASE_DATE"}
    )
    private MovieSort sortBy;
}