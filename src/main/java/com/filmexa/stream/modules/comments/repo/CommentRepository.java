package com.filmexa.stream.modules.comments.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.filmexa.stream.modules.comments.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query(
            value = "SELECT c FROM Comment c JOIN FETCH c.user WHERE c.movieId = :movieId",
            countQuery = "SELECT count(c) FROM Comment c WHERE c.movieId = :movieId"
    )
    Page<Comment> findByMovieId(@Param("movieId") Long movieId, Pageable pageable);

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Comment> findByIdWithUser(@Param("id") UUID id);
}
