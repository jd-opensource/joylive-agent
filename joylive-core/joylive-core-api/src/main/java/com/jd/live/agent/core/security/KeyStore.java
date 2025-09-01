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

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.io.IOException;

/**
 * Secure storage interface for cryptographic key material.
 * <p>
 * Implementations typically integrate with HSMs, KMS, or secure vaults.
 * Returned keys are in raw encoded format (typically PKCS#8 for private keys
 * and X.509 for public keys).
 */
@Extensible("KeyStore")
public interface KeyStore {

    String TYPE_FILE = "file";

    String TYPE_CLASSPATH = "classpath";

    int ORDER_FILE = 100;

    int ORDER_CLASSPATH = ORDER_FILE + 100;

    /**
     * Retrieves encoded private key bytes.
     *
     * @param keyId Unique identifier for the key (e.g. "signing-key/v1")
     * @return PKCS#8 encoded private key
     * @throws IOException If key retrieval fails
     */
    byte[] getPrivateKey(String keyId) throws IOException;

    /**
     * Retrieves encoded public key bytes.
     *
     * @param keyId Corresponding identifier for the private key
     * @return X.509 encoded public key
     * @throws IOException If key retrieval fails
     */
    byte[] getPublicKey(String keyId) throws IOException;
}
