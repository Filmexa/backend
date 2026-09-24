/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   InvalidPaginationException.java                    :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/17 10:23:59 by marouan           #+#    #+#             */
/*   Updated: 2026/09/17 10:29:35 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.common.exception;

import java.lang.RuntimeException;

public class InvalidPaginationException extends RuntimeException{
    public InvalidPaginationException( String message ) {
        super( message );
    }
}
