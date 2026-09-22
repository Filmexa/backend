package com.filmexa.stream.modules.download.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DownloadRequestDto {

    @NotNull(message = "Movie ID is required")
    private UUID movieId;

    @NotBlank(message = "Magnet URL is required")
    private String magnetUrl;
}
