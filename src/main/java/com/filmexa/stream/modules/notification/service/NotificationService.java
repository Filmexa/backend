/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   NotificationService.java                           :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/14 21:59:26 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 17:02:00 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.notification.service;

import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.users.entity.User;

@Service
public interface NotificationService {

    void sendVerificationCode(User user, String code, long expirationMinutes);

    void sendPasswordResetCode(User user, String code, long expirationMinutes);

    void sendEmailChangeCode(User user, String newEmail, String code, long expirationMinutes);
}
