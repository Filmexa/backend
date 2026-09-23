package com.filmexa.stream.modules.myList.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MyListItemResponse {

    private UUID id;

    private Long movieId;

    private String title;

    private String posterUrl;

    private String releaseDate;

    private Double rating;

    private LocalDateTime addedAt;
}
