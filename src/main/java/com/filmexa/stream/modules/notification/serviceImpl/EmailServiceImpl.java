/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   EmailServiceImpl.java                              :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/14 21:59:31 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 17:02:11 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.notification.serviceImpl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import com.filmexa.stream.modules.notification.service.NotificationService;
import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.PreferredLanguage;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender, ITemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendVerificationCode(User user, String code, long expirationMinutes) {
        String subject = switch (user.getPreferredLanguage()) {
            case FRENCH -> "Vérifiez votre compte Filmexa";
            case ARABIC -> "تحقق من حساب Filmexa الخاص بك";
            default -> "Verify your Filmexa account";
        };
        send(user, user.getEmail(), "mail/verification-" + user.getPreferredLanguage().getDisplayName(), subject, code, expirationMinutes);
    }

    @Override
    public void sendPasswordResetCode(User user, String code, long expirationMinutes) {
        String lang = languageCode(user.getPreferredLanguage());
        String subject = switch (lang) {
            case "fr" -> "Réinitialisez votre mot de passe Filmexa";
            case "ar" -> "إعادة تعيين كلمة مرور Filmexa";
            default -> "Reset your Filmexa password";
        };
        send(user, user.getEmail(), "mail/password-reset-" + lang, subject, code, expirationMinutes);
    }

    @Override
    public void sendEmailChangeCode(User user, String newEmail, String code, long expirationMinutes) {
        String lang = languageCode(user.getPreferredLanguage());
        String subject = switch (lang) {
            case "fr" -> "Confirmez votre nouvelle adresse e-mail Filmexa";
            case "ar" -> "تأكيد بريدك الإلكتروني الجديد في Filmexa";
            default -> "Confirm your new Filmexa email address";
        };
        send(user, newEmail, "mail/email-change-" + lang, subject, code, expirationMinutes);
    }

    private void send(User user, String to, String template, String subject, String code, long expirationMinutes) {
        Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("code", code);
        context.setVariable("expirationMinutes", expirationMinutes);

        String html = templateEngine.process(template, context);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send email to " + to, e);
        }
    }

    private String languageCode(PreferredLanguage language) {
        if (language == null) {
            return "en";
        }
        return switch (language) {
            case FRENCH -> "fr";
            case ARABIC -> "ar";
            case ENGLISH -> "en";
        };
    }
}
