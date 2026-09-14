package com.filmexa.stream.modules.comments.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.filmexa.stream.modules.comments.dto.CommentResponse;
import com.filmexa.stream.modules.comments.dto.CreateCommentRequest;
import com.filmexa.stream.modules.comments.dto.UpdateCommentRequest;
import com.filmexa.stream.modules.comments.entity.Comment;
import com.filmexa.stream.modules.comments.repo.CommentRepository;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.AuthProvider;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;
import com.filmexa.stream.modules.users.enums.Role;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(commentRepository);
        lenient().when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            if (comment.getId() == null) {
                comment.setId(UUID.randomUUID());
            }
            return comment;
        });
    }

    private User newUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setRole(Role.USER);
        return user;
    }

    private Comment existingComment(Long movieId, User author, String content) {
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setMovieId(movieId);
        comment.setUser(author);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        return comment;
    }

    @Test
    void createComment_shouldPersistTrimmedContent() {
        User author = newUser("alice");
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("  Great movie!  ");

        var response = commentService.createComment(42L, author, request);

        assertThat(response.getMovieId()).isEqualTo(42L);
        assertThat(response.getContent()).isEqualTo("Great movie!");
        assertThat(response.getAuthor().getUsername()).isEqualTo("alice");
        assertThat(response.getId()).isNotNull();
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void getCommentsByMovieId_shouldMapAuthors() {
        User author = newUser("bob");
        Comment comment = existingComment(7L, author, "Nice");
        when(commentRepository.findByMovieId(7L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(comment)));

        Page<CommentResponse> page = commentService.getCommentsByMovieId(7L, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Nice");
        assertThat(page.getContent().get(0).getAuthor().getUsername()).isEqualTo("bob");
    }

    @Test
    void updateComment_shouldUpdateWhenAuthorMatches() {
        User author = newUser("alice");
        Comment comment = existingComment(42L, author, "Old");
        when(commentRepository.findByIdWithUser(comment.getId())).thenReturn(Optional.of(comment));

        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated");

        var response = commentService.updateComment(42L, comment.getId(), author, request);

        assertThat(response.getContent()).isEqualTo("Updated");
    }

    @Test
    void updateComment_shouldRejectOtherUsers() {
        User author = newUser("alice");
        User other = newUser("mallory");
        Comment comment = existingComment(42L, author, "Old");
        when(commentRepository.findByIdWithUser(comment.getId())).thenReturn(Optional.of(comment));

        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Hijacked");

        assertThatThrownBy(() -> commentService.updateComment(42L, comment.getId(), other, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You can only modify your own comments");
    }

    @Test
    void updateComment_shouldReturnNotFoundWhenMovieDoesNotMatch() {
        User author = newUser("alice");
        Comment comment = existingComment(42L, author, "Old");
        when(commentRepository.findByIdWithUser(comment.getId())).thenReturn(Optional.of(comment));

        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated");

        assertThatThrownBy(() -> commentService.updateComment(99L, comment.getId(), author, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Comment not found");
    }

    @Test
    void deleteComment_shouldDeleteWhenAuthorMatches() {
        User author = newUser("alice");
        Comment comment = existingComment(42L, author, "Bye");
        when(commentRepository.findByIdWithUser(comment.getId())).thenReturn(Optional.of(comment));

        commentService.deleteComment(42L, comment.getId(), author);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_shouldNotDeleteWhenAuthorDoesNotMatch() {
        User author = newUser("alice");
        User other = newUser("mallory");
        Comment comment = existingComment(42L, author, "Bye");
        when(commentRepository.findByIdWithUser(comment.getId())).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(42L, comment.getId(), other))
                .isInstanceOf(IllegalArgumentException.class);
        verify(commentRepository, never()).delete(any());
    }
}
