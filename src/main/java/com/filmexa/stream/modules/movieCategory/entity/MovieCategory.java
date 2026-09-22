/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   MovieCategory.java                                 :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/14 11:10:38 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/22 12:28:22 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.movieCategory.entity;

import java.time.LocalDateTime;

import com.filmexa.stream.common.utils.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table
public class MovieCategory extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String nameEn;

    @Column(unique = true, length = 100)
    private String nameFr;

    @Column(unique = true, length = 100)
    private String nameAr;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, unique = true)
    private Long genreId;

    private boolean active = true;

    private Integer displayOrder;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public String getName(String language) {
        if ("fr".equals(language) && nameFr != null) {
            return nameFr;
        }
        if ("ar".equals(language) && nameAr != null) {
            return nameAr;
        }
        return nameEn;
    }
}
