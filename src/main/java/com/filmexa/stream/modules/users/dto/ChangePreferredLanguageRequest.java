/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   ChangePreferredLanguageRequest.java                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/16 15:40:14 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 15:49:20 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.dto;

import com.filmexa.stream.modules.users.enums.PreferredLanguage;

import lombok.Data;

@Data
public class ChangePreferredLanguageRequest {

    private PreferredLanguage preferredLanguage;
}
