/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   PreferredLanguage.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/10 16:27:34 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/10 16:31:16 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.enums;

public enum PreferredLanguage {
    ENGLISH("EN"),
    FRENCH("FR"),
    ARABIC("AR");

    private final String displayName;

    PreferredLanguage(String displayName) {
        this.displayName = displayName;
    }
}
