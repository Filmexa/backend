/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   VerifyEmailRequest.java                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/14 21:40:50 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 14:55:09 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must be less than 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        message = "Email is not valid"
    )
    private String email;

    @NotBlank(message = "Verification code is required")
    @Size(max = 6, message = "Verification code must be exactly 6 characters")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid verification code format")
    private String code;
}
