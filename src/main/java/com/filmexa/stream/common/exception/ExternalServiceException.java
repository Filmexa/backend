/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   ExternalServiceException.java                      :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/25 13:08:39 by marouan           #+#    #+#             */
/*   Updated: 2026/09/25 19:03:36 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.common.exception;

import java.lang.RuntimeException;
import java.lang.Throwable;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
    public ExternalServiceException( String message, Throwable cause ) {
        super( message, cause);
    }
}
