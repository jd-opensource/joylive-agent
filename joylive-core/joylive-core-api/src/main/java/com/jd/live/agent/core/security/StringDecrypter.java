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
package com.jd.live.agent.core.security;

import com.jd.live.agent.core.exception.CipherException;

import java.util.function.Consumer;

/**
 * Decrypts string values.
 */
public interface StringDecrypter {

    String COMPONENT_STRING_DECRYPTER = "stringDecrypter";

    /**
     * Attempts to decrypt the given string value.
     *
     * @param value the string to decrypt
     * @return the decrypted string, or the original value if decryption fails
     */
    String tryDecrypt(String value);

    /**
     * Attempts to decrypt the given string value and executes a callback on success.
     *
     * @param value     the string to decrypt
     * @param onSuccess callback function executed with the decrypted string if decryption succeeds
     */
    void tryDecrypt(String value, Consumer<String> onSuccess);

    /**
     * decrypt the given string value.
     *
     * @param value the string to decrypt
     * @return the decrypted string, or the original value if decryption fails
     */
    String decrypt(String value) throws CipherException;

}
