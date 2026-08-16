/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   ConfirmEmailChangeRequest.java                     :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/16 17:10:18 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 17:11:34 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmEmailChangeRequest {

    @NotBlank(message = "Confirmation code is required")
    @Size(max = 6, message = "Confirmation code must be exactly 6 characters")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid confirmation code format")
    private String code;
}
