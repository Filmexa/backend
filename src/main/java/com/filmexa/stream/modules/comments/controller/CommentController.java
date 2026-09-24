package com.filmexa.stream.modules.comments.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.common.utils.ErrorResponse;
import com.filmexa.stream.modules.comments.dto.CommentResponse;
import com.filmexa.stream.modules.comments.dto.CreateCommentRequest;
import com.filmexa.stream.modules.comments.dto.UpdateCommentRequest;
import com.filmexa.stream.modules.comments.service.CommentService;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.security.ratelimit.RateLimit;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/movie/{movieId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Movie comment APIs")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(commentService.getCommentsByMovieId(movieId, pageRequest));
    }

    @PostMapping
    @RateLimit(limit = 20, windowSeconds = 60)
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long movieId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse created = commentService.createComment(movieId, currentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long movieId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        try {
            return ResponseEntity.ok(commentService.updateComment(movieId, commentId, currentUser(), request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long movieId,
            @PathVariable UUID commentId) {
        try {
            commentService.deleteComment(movieId, commentId, currentUser());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        }
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
