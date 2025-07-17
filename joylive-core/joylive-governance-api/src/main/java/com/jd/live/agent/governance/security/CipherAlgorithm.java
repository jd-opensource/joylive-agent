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
 * Cryptographic algorithm for byte-level encryption/decryption operations.
 */
public interface CipherAlgorithm {

    String KEY_CIPHER_ALGORITHM = "cipher.algorithm";

    String KEY_CIPHER_PASSWORD = "cipher.password";

    String KEY_CIPHER_ITERATIONS = "cipher.iterations";

    String KEY_CIPHER_SALT_GENERATOR = "cipher.saltGenerator";

    String KEY_CIPHER_CODEC = "cipher.codec";

    String KEY_CIPHER_SALT = "cipher.salt";

    String ENV_CIPHER_CODEC = "CONFIG_CENTER_CIPHER_CODEC";

    String ENV_CIPHER_ALGORITHM = "CONFIG_CENTER_CIPHER_ALGORITHM";

    String ENV_CIPHER_PASSWORD = "CONFIG_CENTER_CIPHER_PASSWORD";

    String ENV_CIPHER_ITERATIONS = "CONFIG_CENTER_CIPHER_ITERATIONS";

    String ENV_CIPHER_SALT = "CONFIG_CENTER_CIPHER_SALT";

    String CIPHER_DEFAULT_ALGORITHM = "PBEWithMD5AndDES";

    int CIPHER_DEFAULT_ITERATIONS = 1000;

    /**
     * Decrypts data to its original form.
     *
     * @param data encrypted byte array
     * @return decrypted byte array
     * @throws Exception if decryption fails
     */
    byte[] decrypt(byte[] data) throws Exception;

    /**
     * Encrypts data for secure storage/transmission.
     *
     * @param data original byte array
     * @return encrypted byte array
     * @throws Exception if encryption fails
     */
    byte[] encrypt(byte[] data) throws Exception;
}
