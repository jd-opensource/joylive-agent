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
package com.jd.live.agent.implement.cipher.jasypt;

import com.jd.live.agent.governance.security.Cipher;
import com.jd.live.agent.governance.security.CipherAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cipher implementation that combines {@link CipherAlgorithm} with Base64 encoding.
 * <p>Provides string-based encryption/decryption by handling charset conversion and Base64 wrapping.
 */
public class JasyptCipher implements Cipher {

    private final CipherAlgorithm algorithm;

    public JasyptCipher(CipherAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String encrypt(String source) throws Exception {
        return Base64.getEncoder().encodeToString(algorithm.encrypt(source.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String decrypt(String encoded) throws Exception {
        return new String(algorithm.decrypt(Base64.getDecoder().decode(encoded)), StandardCharsets.UTF_8);
    }
}
