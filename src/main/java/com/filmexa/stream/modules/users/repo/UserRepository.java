/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserRepository.java                                :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/08 18:13:31 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/17 12:31:48 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.enums.AuthProvider;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
}
