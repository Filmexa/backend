/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   Window.java                                        :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/31 18:27:40 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/31 18:41:37 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.security.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Window {
    public final AtomicLong resetAt;
    public final AtomicInteger count = new AtomicInteger(0);

    Window(long resetAt) {
        this.resetAt = new AtomicLong(resetAt);
    }
}
