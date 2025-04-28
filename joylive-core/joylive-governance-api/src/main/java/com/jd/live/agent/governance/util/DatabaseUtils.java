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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class DatabaseUtils {

    public static final ThreadLocal<String> ADDRESS = new ThreadLocal<>();

    public static final Map<String, AtomicReference<String>> REDIRECTS = new ConcurrentHashMap<>();

    public static void redirect(String oldAddress, String newAddress, BiConsumer<String, String> callback) {
        AtomicReference<String> reference = REDIRECTS.computeIfAbsent(oldAddress, AtomicReference::new);
        String old = reference.get();
        if (!newAddress.equals(old) && reference.compareAndSet(old, newAddress)) {
            callback.accept(old, newAddress);
        }
    }

}
