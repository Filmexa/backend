/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   TokenType.java                                     :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/16 17:14:14 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 17:29:17 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.enums;

public enum TokenType {
    EMAIL_VERIFICATION("email_verification"),
    PASSWORD_RESET("password_reset"),
    EMAIL_CHANGE("email_change");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
