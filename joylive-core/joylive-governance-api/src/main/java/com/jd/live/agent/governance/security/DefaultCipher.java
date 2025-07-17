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
        this.codec = codec;
    }

    @Override
    public String encrypt(String source) throws Exception {
        return codec.encode(algorithm.encrypt(source.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String decrypt(String encoded) throws Exception {
        return new String(algorithm.decrypt(codec.decode(encoded)), StandardCharsets.UTF_8);
    }
}
