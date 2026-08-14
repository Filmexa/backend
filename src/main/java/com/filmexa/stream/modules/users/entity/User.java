/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   User.java                                          :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:09:41 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/14 19:15:45 by kchaouki         ###   ########.fr       */
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
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User extends AbstractEntity implements UserDetails {

    @Column(unique = true)
    private String username;

    @JsonIgnore
    private String hashedPassword;

    private String firstName;

    private String lastName;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    @Column
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider;

    @Column
    private String providerId;

    @Column
    private String emailVerificationCode;

    @Column
    private LocalDateTime emailVerificationCodeExpiresAt;

    @Column(columnDefinition = "TEXT")
    private String bio;

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
