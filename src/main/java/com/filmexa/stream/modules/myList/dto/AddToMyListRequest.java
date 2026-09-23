package com.filmexa.stream.modules.myList.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AddToMyListRequest {

    @NotNull(message = "movieId is required")
    @Positive(message = "movieId must be greater than 0")
    private Long movieId;

    @Pattern(
        regexp = "^(en|fr|ar)$",
        message = "Language must be en, fr, or ar"
    )
    private String language = "en";
}
