/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   ConflictException.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/22 11:21:55 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/22 11:21:55 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.common.exception;

import java.lang.RuntimeException;

public class ConflictException extends RuntimeException{
    public ConflictException( String message ) {
        super( message );
    }
}
