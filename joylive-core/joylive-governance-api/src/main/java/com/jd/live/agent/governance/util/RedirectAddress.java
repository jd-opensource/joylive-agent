/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.governance.util;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Thread-safe utility for managing database address redirections.
 *
 * <p>Maintains both thread-local and global redirect mappings with atomic updates.
 * Primarily used for database failover scenarios.
 */
@Getter
public class RedirectAddress {

    private static final ThreadLocal<RedirectAddress> ADDRESS = new ThreadLocal<>();

    private static final Map<String, AtomicReference<String>> REDIRECTS = new ConcurrentHashMap<>();

    private final String oldAddress;

    private final String newAddress;

    public RedirectAddress(String oldAddress, String newAddress) {
        this.oldAddress = oldAddress;
        this.newAddress = newAddress;
    }

    public RedirectAddress newAddress(String newAddress) {
        return new RedirectAddress(oldAddress, newAddress);
    }

    /**
     * Retrieves and clears the thread-local redirect address
     *
     * @return Current redirect address or null
     */
    public static RedirectAddress getAndRemove() {
        RedirectAddress address = ADDRESS.get();
        ADDRESS.remove();
        return address;
    }

    /**
     * Sets a thread-local redirect address
     *
     * @param address Contains old/new address pair
     */
    public static void setAddress(RedirectAddress address) {
        ADDRESS.set(address);
    }

    /**
     * Atomically updates global redirect mapping
     *
     * @param address  Original database address
     * @param callback Notification when redirection occurs
     */
    public static void redirect(RedirectAddress address, BiConsumer<String, String> callback) {
        AtomicReference<String> reference = REDIRECTS.computeIfAbsent(address.getOldAddress(), AtomicReference::new);
        String oldRedirect = reference.get();
        String newRedirect = address.getNewAddress();
        if (!newRedirect.equals(oldRedirect) && reference.compareAndSet(oldRedirect, newRedirect)) {
            callback.accept(oldRedirect, newRedirect);
        }
    }

}
