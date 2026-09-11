/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   NotFoundException.java                             :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: maddou <maddou@student.42.fr>              +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/09/11 15:25:13 by maddou            #+#    #+#             */
/*   Updated: 2026/09/11 15:25:14 by maddou           ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.common.exception;

import java.lang.RuntimeException;

public class NotFoundException extends RuntimeException{
    public NotFoundException( String message ) {
        super( message );
    }
}
