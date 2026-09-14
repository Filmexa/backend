package com.filmexa.stream.modules.movie.entity;

import com.filmexa.stream.common.utils.AbstractEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data 
@Table
public class Movie extends AbstractEntity{
    private String name;
}
