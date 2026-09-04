/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   RateLimiterService.java                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: kchaouki <kchaouki@student.1337.ma>        +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/31 17:59:11 by kchaouki          #+#    #+#             */
/*   Updated: 2026/08/31 18:36:48 by kchaouki         ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.security.ratelimit;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;

        Window window = windows.computeIfAbsent(key, k -> new Window(now + windowMillis));

        synchronized (window) {
            if (now >= window.resetAt.get()) {
                window.resetAt.set(now + windowMillis);
                window.count.set(0);
            }
            if (window.count.get() >= limit) {
                return false;
            }
            window.count.incrementAndGet();
            return true;
        }
    }
}
