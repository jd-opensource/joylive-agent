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
package com.jd.live.agent.plugin.router.springcloud.v2_1.util;

import feign.Request;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A utility class for handling Feign-related operations, specifically checking for the presence of the {@code requestTemplate} method.
 */
public class FeignUtils {

    // Volatile flag to ensure thread-safe access to the requestTemplate status
    private static volatile AtomicBoolean requestTemplate;

    // Mutex object for synchronization
    private static final Object mutex = new Object();

    /**
     * Checks whether the {@code requestTemplate} method is present in the {@link Request} class.
     * This method uses double-checked locking to ensure thread safety and lazy initialization.
     *
     * @return {@code true} if the {@code requestTemplate} method is present, {@code false} otherwise.
     */
    public static boolean withRequestTemplate() {
        if (requestTemplate == null) {
            synchronized (mutex) {
                if (requestTemplate == null) {
                    AtomicBoolean flag = new AtomicBoolean(false);
                    try {
                        // Attempt to find the requestTemplate method in the Request class
                        Request.class.getDeclaredMethod("requestTemplate");
                        flag.set(true);
                    } catch (NoSuchMethodException ignored) {
                        // Method not found, flag remains false
                    }
                    requestTemplate = flag;
                }
            }
        }
        return requestTemplate.get();
    }
}
