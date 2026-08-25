/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserController.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:24:35 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/24 18:10:02 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.controller;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.modules.users.dto.AvatarFile;
import com.filmexa.stream.modules.users.dto.ChangeEmailRequest;
import com.filmexa.stream.modules.users.dto.ChangePreferredLanguageRequest;
import com.filmexa.stream.modules.users.dto.ConfirmEmailChangeRequest;
import com.filmexa.stream.modules.users.dto.UpdateProfileRequest;
import com.filmexa.stream.modules.users.dto.UserProfileResponse;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.service.AvatarService;
import com.filmexa.stream.modules.users.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {
    private final UserService userService;
    private final AvatarService avatarService;

    @PostMapping("/change-preferred-language")
    public ResponseEntity<?>  changePreferredLanguage(@RequestBody ChangePreferredLanguageRequest request) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();
        System.out.println("Authenticated username: " + username);

        userService.changePreferredLanguage(
                username,
                request.getPreferredLanguage()
        );

        return ResponseEntity.ok(
                "Preferred language changed to: " + request.getPreferredLanguage()
        );
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@Valid @RequestBody ChangeEmailRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            userService.requestEmailChange(username, request.getNewEmail());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }

        return ResponseEntity.ok("Please check your new email for a confirmation code.");
    }

    @PostMapping("/change-email/confirm")
    public ResponseEntity<?> confirmChangeEmail(@Valid @RequestBody ConfirmEmailChangeRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        boolean confirmed = userService.confirmEmailChange(username, request.getCode());
        if (!confirmed) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired confirmation code");
        }

        return ResponseEntity.ok("Email changed successfully");
    }

    @GetMapping("/me/avatar")
    public ResponseEntity<byte[]> getMyAvatar() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return respondWithAvatar(user.getProfilePictureUrl());
    }

    @PutMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public ResponseEntity<?> updateMyAvatar(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        try {
            avatarService.saveAvatar(user.getId(), file);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        return ResponseEntity.ok("Avatar updated successfully");
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable UUID userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return respondWithAvatar(user.getProfilePictureUrl());
    }

    private ResponseEntity<byte[]> respondWithAvatar(String pictureUrl) {
        try {
            AvatarFile avatar = avatarService.loadAvatar(pictureUrl);
            return ResponseEntity.ok()
                    .contentType(avatar.getContentType())
                    .body(avatar.getData());
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/profiles")
    public ResponseEntity<Page<UserProfileResponse>> getAllProfiles(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
        Page<UserProfileResponse> profiles = userService.getAllUsers(PageRequest.of(page, size))
                .map(this::toProfileResponse);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new NoSuchElementException("User not found"));
            return ResponseEntity.ok(toProfileResponse(user));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<?> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            User updated = userService.updateProfile(username, request);
            return ResponseEntity.ok(toProfileResponse(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    private UserProfileResponse toProfileResponse(User user) {
        String avatarUrl = user.getProfilePictureUrl() != null
                ? "/api/users/" + user.getId() + "/avatar"
                : null;

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                avatarUrl
        );
    }
}
