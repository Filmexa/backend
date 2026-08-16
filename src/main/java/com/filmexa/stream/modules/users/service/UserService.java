/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserService.java                                   :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:23:06 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 19:42:52 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.auth.dto.RegisterRequest;
import com.filmexa.stream.modules.auth.dto.ResetPasswordRequest;
import com.filmexa.stream.modules.users.dto.UpdateProfileRequest;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.AuthProvider;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;
import com.filmexa.stream.modules.users.enums.Role;

@Service
public interface UserService {

    public Optional<User> findById(UUID userId);

    public Optional<User> findByUsername(String username);

    public User registerUser(RegisterRequest user);

    public boolean verifyEmail(String email, String code);

    public void resendVerificationCode(String email);

    public void requestPasswordReset(String email);

    public void resendPasswordResetCode(String email);

    public boolean resetPassword(ResetPasswordRequest request);

    public void saveRefreshToken(String username, String refreshToken);

    public void logout(String username);

    public void changePreferredLanguage(String username, PreferredLanguage preferredLanguage);

    public void requestEmailChange(String username, String newEmail);

    public boolean confirmEmailChange(String username, String code);

    public User updateProfile(String username, UpdateProfileRequest request);

    public Page<User> getAllUsers(Pageable pageable);
}
