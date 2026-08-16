/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UpdateProfileRequest.java                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/16 19:20:00 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 19:26:11 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "First name must be less than 50 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "First name can only contain letters, numbers, underscores and hyphens"
    )
    private String firstName;

    @Size(max = 50, message = "Last name must be less than 50 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Last name can only contain letters, numbers, underscores and hyphens"
    )
    private String lastName;

    @Size(max = 20, message = "Phone number must be less than 20 characters")
    @Pattern(
        regexp = "^[0-9+\\-\\s]+$",
        message = "Phone number is not valid"
    )
    private String phoneNumber;
}
