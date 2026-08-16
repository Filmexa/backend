/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserService.java                                   :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:23:06 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 15:45:13 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.auth.dto.RegisterRequest;
import com.filmexa.stream.modules.auth.dto.ResetPasswordRequest;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;

@Service
public interface UserService {

    public String getUserGreeting(String username);

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
}
