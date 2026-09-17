package com.filmexa.stream.modules.comments.serviceImpl;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.filmexa.stream.modules.comments.dto.CommentResponse;
import com.filmexa.stream.modules.comments.dto.CreateCommentRequest;
import com.filmexa.stream.modules.comments.dto.UpdateCommentRequest;
import com.filmexa.stream.modules.comments.entity.Comment;
import com.filmexa.stream.modules.comments.repo.CommentRepository;
import com.filmexa.stream.modules.comments.service.CommentService;
import com.filmexa.stream.modules.users.dto.UserInfosSimpleResponse;
import com.filmexa.stream.modules.users.entity.User;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;

    public CommentServiceImpl(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByMovieId(Long movieId, Pageable pageable) {
        return commentRepository.findByMovieId(movieId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public CommentResponse createComment(Long movieId, User author, CreateCommentRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setMovieId(movieId);
        comment.setUser(author);
        comment.setContent(request.getContent().trim());
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        return toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long movieId, UUID commentId, User currentUser, UpdateCommentRequest request) {
        Comment comment = getOwnedComment(movieId, commentId, currentUser);
        comment.setContent(request.getContent().trim());
        comment.setUpdatedAt(LocalDateTime.now());
        return toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long movieId, UUID commentId, User currentUser) {
        Comment comment = getOwnedComment(movieId, commentId, currentUser);
        commentRepository.delete(comment);
    }

    private Comment getOwnedComment(Long movieId, UUID commentId, User currentUser) {
        Comment comment = commentRepository.findByIdWithUser(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));

        if (!comment.getMovieId().equals(movieId)) {
            throw new NoSuchElementException("Comment not found");
        }

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You can only modify your own comments");
        }

        return comment;
    }

    private CommentResponse toResponse(Comment comment) {
        User author = comment.getUser();
        return new CommentResponse(
                comment.getId(),
                comment.getMovieId(),
                comment.getContent(),
                new UserInfosSimpleResponse(author.getId(), author.getUsername()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
