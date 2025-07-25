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
package com.jd.live.agent.governance.security;

import com.jd.live.agent.governance.exception.CipherException;

/**
 * Generates cryptographic parameters (salts and initialization vectors).
 *
 * <p>Provides control over whether generated parameters should be included
 * in cryptographic operation results.
 */
public interface CipherGenerator {

    /**
     * Generates a new cryptographic parameter.
     *
     * @param size the length in bytes (must be positive)
     * @return newly generated bytes (never null)
     * @throws CipherException if generation fails
     */
    byte[] create(int size) throws CipherException;

    /**
     * Determines if generated parameters should be included in results.
     *
     * @return true for embedded parameters, false otherwise
     */
    default boolean withResult() {
        return false;
    }

    /**
     * Checks if the value is fixed (immutable).
     *
     * @return {@code true} if the value is fixed, {@code false} otherwise.
     */
    default boolean isFixed() {
        return false;
    }

    default boolean isEmpty() {
        return false;
    }

    int size();
}
