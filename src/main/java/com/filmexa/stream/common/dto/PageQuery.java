/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   PageQuery.java                                     :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/16 15:51:47 by marouan           #+#    #+#             */
/*   Updated: 2026/09/16 15:53:45 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.common.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageQuery {
    @Min(value = 1, message = "Page must be at least 1")
    @Max(value = 500, message = "Page must not exceed 500")
    Integer page;
}
