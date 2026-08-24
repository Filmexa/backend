package com.filmexa.stream.modules.notification.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.IContext;

import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.AuthProvider;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;
import com.filmexa.stream.modules.users.enums.Role;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ITemplateEngine templateEngine;

    private EmailServiceImpl emailService;
    private User user;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender, templateEngine);
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@filmexa.com");

        user = new User();
        user.setUsername("johndoe");
        user.setEmail("johndoe@example.com");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRole(Role.USER);
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
    }

    private MimeMessage newMimeMessage() {
        return new MimeMessage(Session.getDefaultInstance(new java.util.Properties()));
    }

    @Test
    void sendVerificationCode_shouldRenderTemplateAndSendMail() {
        MimeMessage mimeMessage = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mail/verification-en"), any(IContext.class))).thenReturn("<html>body</html>");

        emailService.sendVerificationCode(user, "123456", 15);

        verify(templateEngine).process(eq("mail/verification-en"), any(IContext.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendVerificationCode_shouldUseFrenchTemplate_whenPreferredLanguageFrench() {
        user.setPreferredLanguage(PreferredLanguage.FRENCH);
        MimeMessage mimeMessage = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>body</html>");

        emailService.sendVerificationCode(user, "123456", 15);

        verify(templateEngine).process(eq("mail/verification-fr"), any(IContext.class));
    }

    @Test
    void sendPasswordResetCode_shouldUseArabicTemplate_whenPreferredLanguageArabic() {
        user.setPreferredLanguage(PreferredLanguage.ARABIC);
        MimeMessage mimeMessage = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>body</html>");

        emailService.sendPasswordResetCode(user, "654321", 10);

        verify(templateEngine).process(eq("mail/password-reset-ar"), any(IContext.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmailChangeCode_shouldSendToNewEmail() throws jakarta.mail.MessagingException {
        MimeMessage mimeMessage = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("mail/email-change-en"), any(IContext.class))).thenReturn("<html>body</html>");

        emailService.sendEmailChangeCode(user, "new@example.com", "111222", 5);

        verify(templateEngine).process(eq("mail/email-change-en"), any(IContext.class));
        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getAllRecipients()).isNotNull();
    }

    @Test
    void send_shouldThrowIllegalStateException_whenRecipientAddressIsInvalid() {
        user.setEmail("invalid address with spaces");
        MimeMessage mimeMessage = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>body</html>");

        assertThatThrownBy(() -> emailService.sendVerificationCode(user, "123456", 15))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to send email");
    }
}
