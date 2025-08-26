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

/**
 * Cryptographic algorithm for byte-level encryption/decryption operations.
 */
public interface CipherAlgorithm {

    String CIPHER_DEFAULT_ALGORITHM = "PBEWithMD5AndDES";

    /**
     * Decrypts data to its original form.
     *
     * @param data encrypted byte array
     * @return decrypted byte array
     * @throws CipherException if decryption fails
     */
    byte[] decrypt(byte[] data) throws CipherException;

    /**
     * Encrypts data for secure storage/transmission.
     *
     * @param data original byte array
     * @return encrypted byte array
     * @throws CipherException if encryption fails
     */
    byte[] encrypt(byte[] data) throws CipherException;
}
