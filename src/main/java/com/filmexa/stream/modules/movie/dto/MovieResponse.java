package com.filmexa.stream.modules.movie.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class MovieResponse {
    private UUID id;
    private String name;
}
