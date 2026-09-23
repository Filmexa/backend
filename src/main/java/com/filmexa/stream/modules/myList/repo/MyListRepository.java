package com.filmexa.stream.modules.myList.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.filmexa.stream.modules.myList.entity.MyListItem;

@Repository
public interface MyListRepository extends JpaRepository<MyListItem, UUID> {

    @Query(
            value = "SELECT i FROM MyListItem i WHERE i.user.id = :userId",
            countQuery = "SELECT count(i) FROM MyListItem i WHERE i.user.id = :userId"
    )
    Page<MyListItem> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    Optional<MyListItem> findByUserIdAndMovieId(UUID userId, Long movieId);

    boolean existsByUserIdAndMovieId(UUID userId, Long movieId);
}
