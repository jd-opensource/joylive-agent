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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.security.Cipher;
import com.jd.live.agent.governance.security.CipherAlgorithm;
import com.jd.live.agent.governance.security.CipherAlgorithmFactory;
import com.jd.live.agent.governance.security.CipherFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link CipherFactory} implementation using Jasypt library.
 */
@Extension("jasypt")
public class JasyptCipherFactory implements CipherFactory {

    private static final Map<String, CipherAlgorithmFactory> factories = new HashMap<>();

    static {
        register(new JasyptPBECipherAlgorithmFactory());
    }

    private static void register(CipherAlgorithmFactory factory) {
        for (String name : factory.getNames()) {
            factories.put(name, factory);
        }
    }

    @Override
    public Cipher create(Map<String, String> config) {
        String algorithm = config == null ? null : config.get(CipherAlgorithm.CIPHER_ALGORITHM);
        algorithm = algorithm == null || algorithm.isEmpty() ? CipherAlgorithm.CIPHER_DEFAULT_ALGORITHM : algorithm;
        CipherAlgorithmFactory factory = factories.get(algorithm);
        CipherAlgorithm ca = factory == null ? null : factory.create(config);
        return ca == null ? null : new JasyptCipher(ca);
    }
}
