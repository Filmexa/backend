package com.filmexa.stream.modules.comments.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.filmexa.stream.modules.users.dto.UserInfosSimpleResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommentResponse {

    private UUID id;

    private Long movieId;

    private String content;

    private UserInfosSimpleResponse author;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
