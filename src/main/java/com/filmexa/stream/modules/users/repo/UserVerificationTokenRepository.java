/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   UserVerificationTokenRepository.java               :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/16 17:14:44 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/16 17:29:21 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.modules.users.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.filmexa.stream.modules.users.entity.User;
import com.filmexa.stream.modules.users.entity.UserVerificationToken;
import com.filmexa.stream.modules.users.enums.TokenType;

@Repository
public interface UserVerificationTokenRepository extends JpaRepository<UserVerificationToken, UUID> {
    Optional<UserVerificationToken> findByUserAndType(User user, TokenType type);
    void deleteByUserAndType(User user, TokenType type);
}
