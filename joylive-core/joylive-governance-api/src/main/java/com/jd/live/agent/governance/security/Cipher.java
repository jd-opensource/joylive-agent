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
 * Defines symmetric string encryption/decryption operations.
 * <p>Implementations should provide reversible transformation of string data.
 */
public interface Cipher {

    /**
     * Encrypts plaintext string.
     * @param plainText the input string to encrypt
     * @return encrypted string (Base64 or hex encoded)
     * @throws CipherException if encryption fails
     */
    String encrypt(String plainText) throws CipherException;

    /**
     * Decrypts encoded string to original plaintext.
     *
     * @param encryptedText the encrypted string (Base64 or hex encoded)
     * @return original plaintext string
     * @throws CipherException if decryption fails
     */
    String decrypt(String encryptedText);

    /**
     * Encrypts binary data.
     * @param plainData the input bytes to encrypt
     * @return encrypted byte array
     * @throws CipherException if encryption fails
     */
    byte[] encrypt(byte[] plainData) throws CipherException;

    /**
     * Decrypts binary data to original content.
     *
     * @param encryptedData the encrypted byte array
     * @return original plaintext bytes
     * @throws CipherException if decryption fails
     */
    byte[] decrypt(byte[] encryptedData) throws CipherException;
}
