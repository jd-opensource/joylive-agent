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

import java.util.Map;
import java.util.function.Predicate;

/**
 * Detects and handles encrypted string content.
 */
public interface CipherDetector {

    String CIPHER_PREFIX = "cipher.prefix";

    String CIPHER_SUFFIX = "cipher.suffix";

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

    /**
     * Default implementation of {@link CipherDetector} that uses prefix/suffix markers.
     * <p>Detects encryption by checking for wrapping markers and removes them during unwrap.
     */
    class DefaultCipherDetector implements CipherDetector {

        private final String prefix;

        private final String suffix;

        private final Predicate<String> predicate;

        public DefaultCipherDetector(Map<String, String> config) {
            this.prefix = config == null ? CIPHER_DEFAULT_PREFIX : config.getOrDefault(CIPHER_PREFIX, CIPHER_DEFAULT_PREFIX);
            this.suffix = config == null ? CIPHER_DEFAULT_SUFFIX : config.getOrDefault(CIPHER_SUFFIX, CIPHER_DEFAULT_SUFFIX);
            int prefixLen = prefix.length();
            int suffixLen = suffix.length();
            this.predicate = prefixLen + suffixLen == 0
                    ? null
                    : (prefixLen == 0
                    ? s -> s.endsWith(suffix)
                    : (suffixLen == 0
                    ? s -> s.startsWith(prefix)
                    : s -> s.startsWith(prefix) && s.endsWith(prefix)));
        }

        @Override
        public boolean isEncrypted(String key, String data) {
            return data != null && !data.isEmpty() && predicate != null && predicate.test(data);
        }

        @Override
        public String unwrap(String encoded) {
            return encoded.substring(prefix.length(), encoded.length() - suffix.length());
        }
    }
}
