/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserDTO.java                                       :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:41:44 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/08 18:42:42 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.dto;

import lombok.Data;

@Data
public class UserDTO {

    private String username;
    private String firstName;
    private String lastName;
    private String role;
}
