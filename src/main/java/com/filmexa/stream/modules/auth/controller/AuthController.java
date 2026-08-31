/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   AuthController.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/14 15:44:40 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/31 11:50:03 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.auth.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.common.utils.ErrorResponse;
import com.filmexa.stream.modules.auth.dto.AuthRequest;
import com.filmexa.stream.modules.auth.dto.AuthResponse;
import com.filmexa.stream.modules.auth.dto.ForgotPasswordRequest;
import com.filmexa.stream.modules.auth.dto.OAuthUserResponse;
import com.filmexa.stream.modules.auth.dto.RegisterRequest;
import com.filmexa.stream.modules.auth.dto.ResetPasswordRequest;
import com.filmexa.stream.modules.auth.dto.SetPasswordRequest;
import com.filmexa.stream.modules.auth.dto.VerifyEmailRequest;
import com.filmexa.stream.modules.auth.service.ProviderAuthService;
import com.filmexa.stream.modules.auth.service.RefreshTokenCookieService;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.AuthProvider;
import com.filmexa.stream.modules.users.service.UserService;
import com.filmexa.stream.security.service.JwtService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final ProviderAuthService ftOAuthService;
    private final ProviderAuthService googleOAuthService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, JwtService jwtService,
            @Qualifier("ftOAuthServiceImpl") ProviderAuthService ftOAuthService,
            @Qualifier("googleOAuthServiceImpl") ProviderAuthService googleOAuthService,
            RefreshTokenCookieService refreshTokenCookieService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.ftOAuthService = ftOAuthService;
        this.googleOAuthService = googleOAuthService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest, HttpServletResponse response) {

        User existingUser = userService.findByUsername(authRequest.getUsername()).orElse(null);
        if (existingUser != null && existingUser.getHashedPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                            "This account has no password set. Log in with 42 or set a password first."));
        }

        try {
            Authentication authenticationToken = new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword());
            Authentication authenticated = authenticationManager.authenticate(authenticationToken);
            User user = (User) authenticated.getPrincipal();

            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            userService.saveRefreshToken(user.getUsername(), refreshToken);
            refreshTokenCookieService.addCookie(response, refreshToken);

            AuthResponse authResponse = new AuthResponse();
            authResponse.setEmail(user.getEmail());
            authResponse.setAccessToken(accessToken);
            return ResponseEntity.ok(authResponse);

        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Account not verified. Please check your email."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            userService.logout(username);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Logout failed"));
        }
        refreshTokenCookieService.clearCookie(response);
        return ResponseEntity.ok("Logged out successfully");
    }

    @PutMapping("/set-password")
    public ResponseEntity<?> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "User not found"));
        }

        if (user.getHashedPassword() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Password is already set"));
        }

        userService.setPassword(username, request.getPassword());
        return ResponseEntity.ok("Password set successfully");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Registration successful. Please check your email for the verification code.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyEmailRequest request) {
        boolean verified = userService.verifyEmail(request.getEmail(), request.getCode());
        if (!verified) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid or expired verification code"));
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "User with this email not found"));
        }
        userService.resendVerificationCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "User with this email not found"));
        }

        userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-password-reset")
    public ResponseEntity<?> resendPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "User with this email not found"));
        }
        userService.resendPasswordResetCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "User with this email not found"));
        }

        boolean reset = userService.resetPassword(request);
        if (!reset) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid or expired reset code"));
        }
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = refreshTokenCookieService.extractToken(request);

        if (refreshToken == null || !jwtService.validateRefreshToken(refreshToken)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired refresh token"));
        }

        String username = jwtService.extractUsername(refreshToken);

        User user = userService.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "User not found"));
        }

        if (!refreshToken.equals(user.getRefreshToken())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired refresh token"));
        }

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        userService.saveRefreshToken(user.getUsername(), newRefreshToken);
        refreshTokenCookieService.addCookie(response, newRefreshToken);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setEmail(user.getEmail());
        authResponse.setAccessToken(newAccessToken);

        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/42")
    public void loginWith42(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(ftOAuthService.getAuthorizationUrl(request));
    }

    @GetMapping("/42/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            OAuthUserResponse ftUser = ftOAuthService.authenticate(code, state, request);
            User user = userService.findOrCreateOAuthUser(AuthProvider.INTRA, ftUser);

            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            userService.saveRefreshToken(user.getUsername(), refreshToken);
            refreshTokenCookieService.addCookie(response, refreshToken);

            AuthResponse authResponse = new AuthResponse();
            authResponse.setEmail(user.getEmail());
            authResponse.setAccessToken(accessToken);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @GetMapping("/google")
    public void loginWithGoogle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(googleOAuthService.getAuthorizationUrl(request));
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            OAuthUserResponse googleUser = googleOAuthService.authenticate(code, state, request);
            User user = userService.findOrCreateOAuthUser(AuthProvider.GOOGLE, googleUser);

            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            userService.saveRefreshToken(user.getUsername(), refreshToken);
            refreshTokenCookieService.addCookie(response, refreshToken);

            AuthResponse authResponse = new AuthResponse();
            authResponse.setEmail(user.getEmail());
            authResponse.setAccessToken(accessToken);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }
}
