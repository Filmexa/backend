/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserServiceImpl.java                               :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:23:04 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/31 11:03:20 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.serviceImpl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.auth.dto.OAuthUserResponse;
import com.filmexa.stream.modules.auth.dto.RegisterRequest;
import com.filmexa.stream.modules.auth.dto.ResetPasswordRequest;
import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.modules.users.dto.UpdateProfileRequest;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.entity.UserVerificationToken;
import com.filmexa.stream.modules.users.enums.AuthProvider;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;
import com.filmexa.stream.modules.users.enums.Role;
import com.filmexa.stream.modules.users.enums.TokenType;
import com.filmexa.stream.modules.users.repo.UserRepository;
import com.filmexa.stream.modules.users.repo.UserVerificationTokenRepository;
import com.filmexa.stream.modules.users.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Value("${security.verification.expiration-minutes}")
    private long verificationExpirationMinutes;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserVerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User registerUser(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setRole(Role.USER);
        user.setEnabled(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User saved = userRepository.save(user);

        String code = generateVerificationCode();
        upsertToken(saved, TokenType.EMAIL_VERIFICATION, code, null,
                now.plusMinutes(verificationExpirationMinutes));

        notificationService.sendVerificationCode(saved, code, verificationExpirationMinutes);
        return saved;
    }

    @Override
    public boolean verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }
        UserVerificationToken token = tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION)
                .orElse(null);
        if (!isValid(token, code)) {
            return false;
        }

        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        tokenRepository.delete(token);
        return true;
    }

    @Override
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.isEnabled()) {
            return;
        }

        String code = generateVerificationCode();
        upsertToken(user, TokenType.EMAIL_VERIFICATION, code, null,
                LocalDateTime.now().plusMinutes(verificationExpirationMinutes));

        notificationService.sendVerificationCode(user, code, verificationExpirationMinutes);
    }

    @Override
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        String code = generateVerificationCode();
        upsertToken(user, TokenType.PASSWORD_RESET, code, null,
                LocalDateTime.now().plusMinutes(verificationExpirationMinutes));

        notificationService.sendPasswordResetCode(user, code, verificationExpirationMinutes);
    }

    private String generateVerificationCode() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    @Override
    public void resendPasswordResetCode(String email) {
        requestPasswordReset(email);
    }

    @Override
    public boolean resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return false;
        }
        UserVerificationToken token = tokenRepository.findByUserAndType(user, TokenType.PASSWORD_RESET)
                .orElse(null);
        if (!isValid(token, request.getCode())) {
            return false;
        }

        user.setHashedPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        tokenRepository.delete(token);
        return true;
    }

    @Override
    public void saveRefreshToken(String username, String refreshToken) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }

        user.setRefreshToken(refreshToken);
        userRepository.save(user);
    }

    @Override
    public void logout(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }

        user.setRefreshToken(null);
        userRepository.save(user);
    }

    @Override
    public void changePreferredLanguage(String username, PreferredLanguage preferredLanguage) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null)
            return;

        user.setPreferredLanguage(preferredLanguage);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void requestEmailChange(String username, String newEmail) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null)
            return;

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from current email");
        }
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        String code = generateVerificationCode();
        upsertToken(user, TokenType.EMAIL_CHANGE, code, newEmail,
                LocalDateTime.now().plusMinutes(verificationExpirationMinutes));

        notificationService.sendEmailChangeCode(user, newEmail, code, verificationExpirationMinutes);
    }

    @Override
    public boolean confirmEmailChange(String username, String code) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }
        UserVerificationToken token = tokenRepository.findByUserAndType(user, TokenType.EMAIL_CHANGE)
                .orElse(null);
        if (!isValid(token, code) || token.getNewEmail() == null) {
            return false;
        }

        user.setEmail(token.getNewEmail());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        tokenRepository.delete(token);
        return true;
    }

    @Override
    public User updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    private boolean isValid(UserVerificationToken token, String code) {
        if (token == null || code == null) {
            return false;
        }
        if (!token.getToken().equals(code)) {
            return false;
        }
        return token.getExpiresAt() != null && token.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private void upsertToken(User user, TokenType type, String code, String newEmail, LocalDateTime expiresAt) {
        UserVerificationToken token = tokenRepository.findByUserAndType(user, type).orElseGet(() -> {
            UserVerificationToken newToken = new UserVerificationToken();
            newToken.setUser(user);
            newToken.setType(type);
            newToken.setCreatedAt(LocalDateTime.now());
            return newToken;
        });

        token.setToken(code);
        token.setNewEmail(newEmail);
        token.setExpiresAt(expiresAt);
        tokenRepository.save(token);
    }

    @Override
    public User findOrCreateOAuthUser(AuthProvider provider, OAuthUserResponse oauthUser) {
        Optional<User> existingUser =
        userRepository.findByAuthProviderAndProviderId(
                provider,
                oauthUser.getProviderId()
        );

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User user = new User();
        user.setAuthProvider(provider);
        user.setProviderId(oauthUser.getProviderId());

        String username = oauthUser.getUsername();
        user.setUsername(username != null ? username : generateUniqueUsername(oauthUser.getEmail()));

        user.setEmail(oauthUser.getEmail());
        user.setFirstName(oauthUser.getFirstName());
        user.setLastName(oauthUser.getLastName());
        user.setProfilePictureUrl(oauthUser.getImageUrl());

        String phone = oauthUser.getPhone();
        if (phone != null && phone.matches("^[0-9+\\-\\s]+$")) {
            user.setPhoneNumber(phone);
        }

        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    private String generateUniqueUsername(String email) {
        String base = email != null ? email.substring(0, email.indexOf('@') >= 0 ? email.indexOf('@') : email.length()) : "user";
        base = base.replaceAll("[^a-zA-Z0-9_-]", "");
        if (base.length() < 3) {
            base = (base + "user").substring(0, Math.max(3, base.length()));
        }
        if (base.length() > 25) {
            base = base.substring(0, 25);
        }

        String candidate = base;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + RANDOM.nextInt(10_000);
        }
        return candidate;
    }

    @Override
    public void setPassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        user.setHashedPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

}
