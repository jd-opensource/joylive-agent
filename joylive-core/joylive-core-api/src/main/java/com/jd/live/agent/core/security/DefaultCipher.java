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
import com.jd.live.agent.core.security.codec.Base64StringCodec;

import java.nio.charset.StandardCharsets;

/**
 * Combines cryptographic operations with string encoding/decoding.
 * <p>Automatically handles UTF-8 text conversion and Base64 processing.
 */
public class DefaultCipher implements Cipher {

    private final CipherAlgorithm algorithm;

    private final StringCodec codec;

    public DefaultCipher(CipherAlgorithm algorithm, StringCodec codec) {
        this.algorithm = algorithm;
        this.codec = codec == null ? Base64StringCodec.INSTANCE : codec;
    }

    @Override
    public byte[] encrypt(byte[] plainData) throws CipherException {
        return plainData == null ? null : algorithm.encrypt(plainData);
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) throws CipherException {
        return encryptedData == null ? null : algorithm.decrypt(encryptedData);
    }

    @Override
    public String encrypt(String plainText) throws CipherException {
        return plainText == null ? null : codec.encode(algorithm.encrypt(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String decrypt(String encryptedText) throws CipherException {
        return encryptedText == null ? null : new String(algorithm.decrypt(codec.decode(encryptedText)), StandardCharsets.UTF_8);
    }
}
