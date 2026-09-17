package com.filmexa.stream.modules.moviesExternal.dto.request;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
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


    @NotBlank(message = "Query is required")
    @Size(min = 2, max = 100, message = "Query must be between 2 and 100 characters")
    private String query;
} 