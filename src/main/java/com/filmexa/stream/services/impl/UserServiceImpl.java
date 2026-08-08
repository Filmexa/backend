/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserServiceImpl.java                               :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:23:04 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/08 18:43:48 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.filmexa.stream.repo.UserRepository;
import com.filmexa.stream.services.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String getUserGreeting(String username) {
        return "Hello, " + username + "! Welcome to Filmexa.";
    }
}
