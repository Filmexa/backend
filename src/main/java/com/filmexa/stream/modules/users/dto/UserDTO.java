/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserDTO.java                                       :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:41:44 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/11 15:28:57 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.dto;

import lombok.Data;

@Data
public class UserDTO {

    private String username;
    private String firstName;
    private String lastName;
    private String role;
}
