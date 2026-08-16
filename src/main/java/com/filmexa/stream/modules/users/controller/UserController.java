/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserController.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:24:35 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 16:02:53 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.modules.users.dto.ChangeEmailRequest;
import com.filmexa.stream.modules.users.dto.ChangePreferredLanguageRequest;
import com.filmexa.stream.modules.users.dto.ConfirmEmailChangeRequest;
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

    @GetMapping("/greet")
    public String greetUser(String username) {
        return userService.getUserGreeting(username);
    }

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
}
