/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   AuthProvider.java                                  :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/10 16:28:54 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/11 16:31:21 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.enums;

public enum AuthProvider {
    LOCAL("Local"),
    GOOGLE("Google"),
    INTRA("Intra 42");

    private final String displayName;

    AuthProvider(String displayName) {
        this.displayName = displayName;
    }
}
