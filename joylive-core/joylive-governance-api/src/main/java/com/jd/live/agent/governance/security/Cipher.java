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
 * Defines symmetric string encryption/decryption operations.
 * <p>Implementations should provide reversible transformation of string data.
 */
public interface Cipher {

    /**
     * Encrypts a plaintext string for secure storage/transmission.
     *
     * @param source the original input string
     * @return encrypted string
     * @throws Exception if encryption fails
     */
    String encrypt(String source) throws Exception;

    /**
     * Decrypts an encoded string back to original content.
     *
     * @param encrypted the encrypted input string
     * @return original plaintext string
     * @throws Exception if decryption fails
     */
    String decrypt(String encrypted) throws Exception;
}
