/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserService.java                                   :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:23:06 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/14 20:56:14 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.filmexa.stream.modules.users.entity.User;

@Service
public interface UserService {

    public String getUserGreeting(String username);

    public Optional<User> findByUsername(String username);
}
