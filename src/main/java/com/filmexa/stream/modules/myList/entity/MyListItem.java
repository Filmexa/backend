package com.filmexa.stream.modules.myList.entity;

import java.time.LocalDateTime;

import com.filmexa.stream.common.utils.AbstractEntity;
import com.filmexa.stream.modules.users.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "my_list_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_my_list_user_movie", columnNames = { "user_id", "movie_id" })
)
public class MyListItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String posterUrl;

    @Column
    private String releaseDate;

    @Column
    private Double rating;

    @Column(nullable = false)
    private LocalDateTime addedAt;
}
