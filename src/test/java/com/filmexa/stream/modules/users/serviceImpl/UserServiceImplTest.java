package com.filmexa.stream.modules.users.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserVerificationTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationService notificationService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, tokenRepository, passwordEncoder, notificationService);
        ReflectionTestUtils.setField(userService, "verificationExpirationMinutes", 15L);
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User newUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("johndoe");
        user.setEmail("johndoe@example.com");
        user.setHashedPassword("hashed-old");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setRole(Role.USER);
        user.setEnabled(false);
        return user;
    }

    // ---- registerUser ----

    @Test
    void registerUser_shouldCreateUserAndSendVerificationCode() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("johndoe");
        request.setEmail("johndoe@example.com");
        request.setPassword("Passw0rd!");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("johndoe@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("hashed-pw");
        when(tokenRepository.findByUserAndType(any(User.class), eq(TokenType.EMAIL_VERIFICATION)))
                .thenReturn(Optional.empty());

        User result = userService.registerUser(request);

        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getHashedPassword()).isEqualTo("hashed-pw");
        assertThat(result.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.isEnabled()).isFalse();

        verify(tokenRepository).save(any(UserVerificationToken.class));
        verify(notificationService).sendVerificationCode(eq(result), anyString(), eq(15L));
    }

    @Test
    void registerUser_shouldThrow_whenUsernameTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("johndoe");
        request.setEmail("johndoe@example.com");
        request.setPassword("Passw0rd!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(newUser()));

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_shouldThrow_whenEmailTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("johndoe@example.com");
        request.setPassword("Passw0rd!");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("johndoe@example.com")).thenReturn(Optional.of(newUser()));

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    // ---- verifyEmail ----

    @Test
    void verifyEmail_shouldReturnFalse_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThat(userService.verifyEmail("ghost@example.com", "123456")).isFalse();
    }

    @Test
    void verifyEmail_shouldReturnFalse_whenTokenInvalidOrExpired() {
        User user = newUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserVerificationToken token = new UserVerificationToken();
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION)).thenReturn(Optional.of(token));

        assertThat(userService.verifyEmail(user.getEmail(), "123456")).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_shouldReturnFalse_whenCodeDoesNotMatch() {
        User user = newUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserVerificationToken token = new UserVerificationToken();
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION)).thenReturn(Optional.of(token));

        assertThat(userService.verifyEmail(user.getEmail(), "000000")).isFalse();
    }

    @Test
    void verifyEmail_shouldEnableUserAndDeleteToken_whenValid() {
        User user = newUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserVerificationToken token = new UserVerificationToken();
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION)).thenReturn(Optional.of(token));

        boolean result = userService.verifyEmail(user.getEmail(), "123456");

        assertThat(result).isTrue();
        assertThat(user.isEnabled()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    // ---- resendVerificationCode ----

    @Test
    void resendVerificationCode_shouldDoNothing_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        userService.resendVerificationCode("ghost@example.com");

        verify(notificationService, never()).sendVerificationCode(any(), any(), anyLong());
    }

    @Test
    void resendVerificationCode_shouldDoNothing_whenUserAlreadyEnabled() {
        User user = newUser();
        user.setEnabled(true);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        userService.resendVerificationCode(user.getEmail());

        verify(notificationService, never()).sendVerificationCode(any(), any(), anyLong());
    }

    @Test
    void resendVerificationCode_shouldSendNewCode_whenUserExistsAndDisabled() {
        User user = newUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION)).thenReturn(Optional.empty());

        userService.resendVerificationCode(user.getEmail());

        verify(tokenRepository).save(any(UserVerificationToken.class));
        verify(notificationService).sendVerificationCode(eq(user), anyString(), eq(15L));
    }

    // ---- requestPasswordReset / resendPasswordResetCode ----

    @Test
    void requestPasswordReset_shouldDoNothing_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        userService.requestPasswordReset("ghost@example.com");

        verify(notificationService, never()).sendPasswordResetCode(any(), any(), anyLong());
    }

    @Test
    void requestPasswordReset_shouldSendCode_whenUserExists() {
        User user = newUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndType(user, TokenType.PASSWORD_RESET)).thenReturn(Optional.empty());

        userService.requestPasswordReset(user.getEmail());

        verify(notificationService).sendPasswordResetCode(eq(user), anyString(), eq(15L));
    }

    @Test
    void resendPasswordResetCode_shouldDelegateToRequestPasswordReset() {
        User user = newUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndType(user, TokenType.PASSWORD_RESET)).thenReturn(Optional.empty());

        userService.resendPasswordResetCode(user.getEmail());

        verify(notificationService).sendPasswordResetCode(eq(user), anyString(), eq(15L));
    }

    // ---- resetPassword ----

    @Test
    void resetPassword_shouldReturnFalse_whenUserNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("ghost@example.com");
        request.setCode("123456");
        request.setNewPassword("NewPassw0rd!");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThat(userService.resetPassword(request)).isFalse();
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndDeleteToken_whenValid() {
        User user = newUser();
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(user.getEmail());
        request.setCode("654321");
        request.setNewPassword("NewPassw0rd!");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        UserVerificationToken token = new UserVerificationToken();
        token.setToken("654321");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(tokenRepository.findByUserAndType(user, TokenType.PASSWORD_RESET)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassw0rd!")).thenReturn("new-hashed");

        boolean result = userService.resetPassword(request);

        assertThat(result).isTrue();
        assertThat(user.getHashedPassword()).isEqualTo("new-hashed");
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    // ---- saveRefreshToken / logout ----

    @Test
    void saveRefreshToken_shouldSetTokenOnUser() {
        User user = newUser();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        userService.saveRefreshToken(user.getUsername(), "refresh-token-value");

        assertThat(user.getRefreshToken()).isEqualTo("refresh-token-value");
        verify(userRepository).save(user);
    }

    @Test
    void saveRefreshToken_shouldDoNothing_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        userService.saveRefreshToken("ghost", "token");

        verify(userRepository, never()).save(any());
    }

    @Test
    void logout_shouldClearRefreshToken() {
        User user = newUser();
        user.setRefreshToken("existing-token");
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        userService.logout(user.getUsername());

        assertThat(user.getRefreshToken()).isNull();
        verify(userRepository).save(user);
    }

    // ---- changePreferredLanguage ----

    @Test
    void changePreferredLanguage_shouldUpdateLanguage() {
        User user = newUser();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        userService.changePreferredLanguage(user.getUsername(), PreferredLanguage.FRENCH);

        assertThat(user.getPreferredLanguage()).isEqualTo(PreferredLanguage.FRENCH);
        verify(userRepository).save(user);
    }

    // ---- requestEmailChange ----

    @Test
    void requestEmailChange_shouldThrow_whenNewEmailSameAsCurrent() {
        User user = newUser();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.requestEmailChange(user.getUsername(), user.getEmail().toUpperCase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be different");
    }

    @Test
    void requestEmailChange_shouldThrow_whenNewEmailAlreadyRegistered() {
        User user = newUser();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(newUser()));

        assertThatThrownBy(() -> userService.requestEmailChange(user.getUsername(), "taken@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void requestEmailChange_shouldSendCode_whenValid() {
        User user = newUser();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_CHANGE)).thenReturn(Optional.empty());

        userService.requestEmailChange(user.getUsername(), "new@example.com");

        verify(notificationService).sendEmailChangeCode(eq(user), eq("new@example.com"), anyString(), eq(15L));
    }

    // ---- confirmEmailChange ----

    @Test
    void confirmEmailChange_shouldReturnFalse_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(userService.confirmEmailChange("ghost", "123456")).isFalse();
    }

    @Test
    void confirmEmailChange_shouldUpdateEmail_whenValid() {
        User user = newUser();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserVerificationToken token = new UserVerificationToken();
        token.setToken("123456");
        token.setNewEmail("new@example.com");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_CHANGE)).thenReturn(Optional.of(token));

        boolean result = userService.confirmEmailChange(user.getUsername(), "123456");

        assertThat(result).isTrue();
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    // ---- updateProfile ----

    @Test
    void updateProfile_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest();
        assertThatThrownBy(() -> userService.updateProfile("ghost", request))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void updateProfile_shouldOnlyUpdateProvidedFields() {
        User user = newUser();
        user.setFirstName("Old");
        user.setLastName("Name");
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("New");

        User result = userService.updateProfile(user.getUsername(), request);

        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getLastName()).isEqualTo("Name");
        verify(userRepository).save(user);
    }

    // ---- findOrCreateOAuthUser ----

    @Test
    void findOrCreateOAuthUser_shouldReturnExistingUser() {
        User existing = newUser();
        OAuthUserResponse oauthUser = new OAuthUserResponse();
        oauthUser.setProviderId("provider-id");

        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.of(existing));

        User result = userService.findOrCreateOAuthUser(AuthProvider.GOOGLE, oauthUser);

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateOAuthUser_shouldCreateNewUser_withUsernameFromOAuth() {
        OAuthUserResponse oauthUser = new OAuthUserResponse();
        oauthUser.setProviderId("provider-id");
        oauthUser.setUsername("oauthuser");
        oauthUser.setEmail("oauth@example.com");
        oauthUser.setFirstName("O");
        oauthUser.setLastName("Auth");
        oauthUser.setImageUrl("https://example.com/pic.jpg");
        oauthUser.setPhone("+123456789");

        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.empty());

        User result = userService.findOrCreateOAuthUser(AuthProvider.GOOGLE, oauthUser);

        assertThat(result.getUsername()).isEqualTo("oauthuser");
        assertThat(result.getEmail()).isEqualTo("oauth@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("+123456789");
        assertThat(result.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getRole()).isEqualTo(Role.USER);
        verify(userRepository).save(result);
    }

    @Test
    void findOrCreateOAuthUser_shouldGenerateUsername_whenOAuthUsernameMissing() {
        OAuthUserResponse oauthUser = new OAuthUserResponse();
        oauthUser.setProviderId("provider-id");
        oauthUser.setUsername(null);
        oauthUser.setEmail("jane.doe@example.com");

        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        User result = userService.findOrCreateOAuthUser(AuthProvider.GOOGLE, oauthUser);

        assertThat(result.getUsername()).isEqualTo("janedoe");
    }

    @Test
    void findOrCreateOAuthUser_shouldIgnoreInvalidPhone() {
        OAuthUserResponse oauthUser = new OAuthUserResponse();
        oauthUser.setProviderId("provider-id");
        oauthUser.setUsername("someuser");
        oauthUser.setEmail("some@example.com");
        oauthUser.setPhone("not-a-phone!!");

        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.empty());

        User result = userService.findOrCreateOAuthUser(AuthProvider.GOOGLE, oauthUser);

        assertThat(result.getPhoneNumber()).isNull();
    }

    // ---- setPassword ----

    @Test
    void setPassword_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.setPassword("ghost", "NewPassw0rd!"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void setPassword_shouldUpdateHashedPassword() {
        User user = newUser();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassw0rd!")).thenReturn("new-hashed");

        userService.setPassword(user.getUsername(), "NewPassw0rd!");

        assertThat(user.getHashedPassword()).isEqualTo("new-hashed");
        verify(userRepository).save(user);
    }

    // ---- findById / findByUsername ----

    @Test
    void findById_shouldDelegateToRepository() {
        User user = newUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(userService.findById(user.getId())).contains(user);
    }

    @Test
    void findByUsername_shouldDelegateToRepository() {
        User user = newUser();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        assertThat(userService.findByUsername(user.getUsername())).contains(user);
    }
}
