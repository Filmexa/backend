package com.filmexa.stream.modules.comments.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.filmexa.stream.modules.comments.dto.CommentResponse;
import com.filmexa.stream.modules.comments.dto.CreateCommentRequest;
import com.filmexa.stream.modules.comments.dto.UpdateCommentRequest;
import com.filmexa.stream.modules.users.entity.User;

public interface CommentService {

    Page<CommentResponse> getCommentsByMovieId(Long movieId, Pageable pageable);

    CommentResponse createComment(Long movieId, User author, CreateCommentRequest request);

    CommentResponse updateComment(Long movieId, UUID commentId, User currentUser, UpdateCommentRequest request);

    void deleteComment(Long movieId, UUID commentId, User currentUser);
}
