
package com.filmexa.stream.modules.moviesExternal.dto.request;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieQuery {
    @Pattern(
        regexp = "^(en|fr|ar)$",
        message = "Language must be en, fr, or ar"
    )
    private String language;
} 