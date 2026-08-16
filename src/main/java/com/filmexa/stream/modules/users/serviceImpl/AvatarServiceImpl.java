/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   AvatarServiceImpl.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/16 18:12:21 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 19:10:58 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.serviceImpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.filmexa.stream.modules.users.dto.AvatarFile;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.repo.UserRepository;
import com.filmexa.stream.modules.users.service.AvatarService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvatarServiceImpl implements AvatarService {

    private static final Map<String, MediaType> ALLOWED_EXTENSIONS = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG,
            "gif", MediaType.IMAGE_GIF,
            "webp", MediaType.valueOf("image/webp")
    );

    private final UserRepository userRepository;

    @Value("${app.img-storage-path}")
    private String imgStoragePath;

    @Override
    public AvatarFile loadAvatar(String pictureUrl) {
        if (pictureUrl == null) {
            throw new NoSuchElementException("No avatar found for this user");
        }

        try {
            byte[] data = Files.readAllBytes(resolve(pictureUrl));
            MediaType contentType = ALLOWED_EXTENSIONS.getOrDefault(
                    extensionOf(pictureUrl), MediaType.APPLICATION_OCTET_STREAM);
            return new AvatarFile(data, contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read avatar file", e);
        }
    }

    @Override
    public void saveAvatar(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.containsKey(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported image format. Allowed formats: " + ALLOWED_EXTENSIONS.keySet());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String relativePath = "avatars/" + userId + "." + extension;

        try {
            Files.createDirectories(resolve("avatars"));

            if (user.getProfilePictureUrl() != null) {
                Files.deleteIfExists(resolve(user.getProfilePictureUrl()));
            }

            file.transferTo(resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save avatar file", e);
        }

        user.setProfilePictureUrl(relativePath);
        userRepository.save(user);
    }

    private Path resolve(String relativePath) {
        return Paths.get(imgStoragePath).resolve(relativePath);
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
