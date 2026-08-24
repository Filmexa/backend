package com.filmexa.stream.modules.users.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.filmexa.stream.modules.users.dto.AvatarFile;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.AuthProvider;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;
import com.filmexa.stream.modules.users.enums.Role;
import com.filmexa.stream.modules.users.repo.UserRepository;

@ExtendWith(MockitoExtension.class)
class AvatarServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private AvatarServiceImpl avatarService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        avatarService = new AvatarServiceImpl(userRepository);
        ReflectionTestUtils.setField(avatarService, "imgStoragePath", tempDir.toString());
    }

    private User newUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("johndoe");
        user.setEmail("johndoe@example.com");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setRole(Role.USER);
        return user;
    }

    @Test
    void loadAvatar_shouldThrow_whenPictureUrlIsNull() {
        assertThatThrownBy(() -> avatarService.loadAvatar(null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void loadAvatar_shouldReadLocalFile() throws IOException {
        Path avatarsDir = tempDir.resolve("avatars");
        Files.createDirectories(avatarsDir);
        Path avatarPath = avatarsDir.resolve("pic.png");
        Files.write(avatarPath, "fake-png-bytes".getBytes(StandardCharsets.UTF_8));

        AvatarFile result = avatarService.loadAvatar("avatars/pic.png");

        assertThat(result.getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(new String(result.getData(), StandardCharsets.UTF_8)).isEqualTo("fake-png-bytes");
    }

    @Test
    void loadAvatar_shouldUseOctetStream_forUnknownExtension() throws IOException {
        Path avatarPath = tempDir.resolve("pic.unknown");
        Files.write(avatarPath, "bytes".getBytes(StandardCharsets.UTF_8));

        AvatarFile result = avatarService.loadAvatar("pic.unknown");

        assertThat(result.getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    void loadAvatar_shouldThrowUncheckedIOException_whenFileMissing() {
        assertThatThrownBy(() -> avatarService.loadAvatar("missing.png"))
                .isInstanceOf(java.io.UncheckedIOException.class);
    }

    @Test
    void saveAvatar_shouldThrow_whenFileIsNull() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> avatarService.saveAvatar(userId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void saveAvatar_shouldThrow_whenFileIsEmpty() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> avatarService.saveAvatar(userId, emptyFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveAvatar_shouldThrow_whenExtensionNotAllowed() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.txt", "text/plain", "data".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> avatarService.saveAvatar(userId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image format");
    }

    @Test
    void saveAvatar_shouldThrow_whenUserNotFound() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "data".getBytes(StandardCharsets.UTF_8));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> avatarService.saveAvatar(userId, file))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void saveAvatar_shouldStoreFileAndUpdateUser_andDeletePreviousAvatar() throws IOException {
        User user = newUser();
        UUID userId = user.getId();

        Path oldAvatarsDir = tempDir.resolve("avatars");
        Files.createDirectories(oldAvatarsDir);
        Path oldAvatarPath = oldAvatarsDir.resolve("old.png");
        Files.write(oldAvatarPath, "old-bytes".getBytes(StandardCharsets.UTF_8));
        user.setProfilePictureUrl("avatars/old.png");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", "new-bytes".getBytes(StandardCharsets.UTF_8));

        avatarService.saveAvatar(userId, file);

        assertThat(Files.exists(oldAvatarPath)).isFalse();
        Path newAvatarPath = tempDir.resolve("avatars/" + userId + ".png");
        assertThat(Files.exists(newAvatarPath)).isTrue();
        assertThat(user.getProfilePictureUrl()).isEqualTo("avatars/" + userId + ".png");
        verify(userRepository).save(user);
    }
}
