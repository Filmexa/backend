package com.filmexa.stream.modules.download.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DownloadRequestDto {

    @NotNull(message = "Movie ID is required")
    @Positive(message = "Movie ID must be greater than 0")
    private Long movieId;

    @NotBlank(message = "Magnet URL is required")
    @Pattern(
        regexp = "^(?i)magnet:\\?xt=urn:btih:.*", 
        message = "Invalid magnet link format. Must start with magnet:?xt=urn:btih:"
    )
    private String magnetUrl;
}
