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
package com.jd.live.agent.core.util;

import java.util.function.Predicate;

/**
 * A utility class for working with exceptions.
 */
public class ExceptionUtils {

    /**
     * Iterates over the exception chain starting from the given throwable and stops when the provided predicate returns false.
     *
     * @param e       the throwable to start iterating from
     * @param predicate a predicate that will be applied to each element in the exception chain
     */
    public static void iterate(Throwable e, Predicate<Throwable> predicate) {
        if (e == null || predicate == null) {
            return;
        }
        Throwable cause = e;
        while (cause != null) {
            if (predicate.test(cause)) {
                cause = cause.getCause();
            } else {
                return;
            }
        }
    }

}
