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

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RestController;

import com.filmexa.stream.modules.users.dto.ChangePreferredLanguageRequest;
import com.filmexa.stream.modules.users.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
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
}
