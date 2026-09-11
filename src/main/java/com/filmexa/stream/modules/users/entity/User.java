/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   User.java                                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:09:41 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/11 20:51:03 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.filmexa.stream.common.utils.AbstractEntity;
import com.filmexa.stream.modules.users.enums.AuthProvider;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;
import com.filmexa.stream.modules.users.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User extends AbstractEntity implements UserDetails {

    @Column(unique = true)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username can only contain letters, numbers, underscores and hyphens"
    )
    private String username;

    @JsonIgnore
    private String hashedPassword;

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

    @Column(unique = true)
    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must be less than 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        message = "Email is not valid"
    )
    private String email;

    @Column(unique = true)
    @Size(max = 20, message = "Phone number must be less than 20 characters")
    @Pattern(
        regexp = "^[0-9+\\-\\s]+$",
        message = "Phone number is not valid"
    )
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider;

    @Column
    private String providerId;

    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreferredLanguage preferredLanguage;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return hashedPassword;
    }

    @Override
    public @Nullable String getUsername() {
        return username;
    }
}
