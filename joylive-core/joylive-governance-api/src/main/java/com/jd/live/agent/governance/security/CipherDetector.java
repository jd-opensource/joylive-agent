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

/**
 * Detects and handles encrypted string content.
 */
public interface CipherDetector {

    String CIPHER_KEYS = "cipher.keys";

    String CIPHER_DEFAULT_PREFIX = "ENC(";

    String CIPHER_DEFAULT_SUFFIX = ")";

    /**
     * Determines if a string appears to be encrypted using the given key.
     *
     * @param key  the encryption key to validate against
     * @param data the string to check for encryption markers
     * @return true if data shows encryption patterns, false otherwise
     */
    boolean isEncrypted(String key, String data);

    /**
     * Checks if the input string appears to be encrypted.
     *
     * @param data the string to check
     * @return true if encrypted, false otherwise
     */
    default boolean isEncrypted(String data) {
        return isEncrypted(null, data);
    }

    /**
     * Removes encryption wrapping from the content.
     *
     * @param encoded the encrypted string
     * @return the unwrapped content
     */
    String unwrap(String encoded);

}
