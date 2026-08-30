/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   ValidationErrorResponse.java                       :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/30 11:11:32 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/30 12:15:56 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.common.exception;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class ValidationErrorResponse {
    int status;
    String message;
    Map<String, String> errors;
}
