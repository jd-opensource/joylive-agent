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

import com.jd.live.agent.governance.config.CipherConfig;
import com.jd.live.agent.governance.security.codec.Base64StringCodec;
import com.jd.live.agent.governance.security.generator.EmptyGeneratorFactory;
import com.jd.live.agent.governance.security.generator.RandomGeneratorFactory;

import java.util.Map;

/**
 * Factory for creating cipher instances with default configuration.
 */
public class DefaultCipherFactory implements CipherFactory {

    private final Map<String, CipherAlgorithmFactory> factories;
    private final Map<String, StringCodec> codecs;
    private final Map<String, CipherGeneratorFactory> salts;

    public DefaultCipherFactory(Map<String, CipherAlgorithmFactory> factories) {
        this(factories, null, null);
    }

    public DefaultCipherFactory(Map<String, CipherAlgorithmFactory> factories,
                                Map<String, StringCodec> codecs,
                                Map<String, CipherGeneratorFactory> salts) {
        this.factories = factories;
        this.codecs = codecs;
        this.salts = salts;
    }

    @Override
    public Cipher create(CipherConfig config) {
        String cipher = config.getCipher();
        CipherAlgorithmContext ctx = createContext(config);
        CipherAlgorithmFactory factory = factories.get(cipher);
        CipherAlgorithm ca = factory == null ? null : factory.create(ctx);
        return ca == null ? null : new DefaultCipher(ca, ctx.getCodec());
    }

    /**
     * Builds Jasypt configuration from input options.
     *
     * @param config Raw configuration input (can be null)
     * @return Fully configured CipherAlgorithmContext instance
     */
    private CipherAlgorithmContext createContext(CipherConfig config) {
        StringCodec codec = codecs == null
                ? Base64StringCodec.INSTANCE
                : codecs.getOrDefault(config.getAlgorithm(), Base64StringCodec.INSTANCE);
        CipherGeneratorFactory saltFactory = salts == null
                ? RandomGeneratorFactory.INSTANCE :
                salts.getOrDefault(config.getSalt(), RandomGeneratorFactory.INSTANCE);
        CipherGeneratorFactory ivFactory = salts == null
                ? EmptyGeneratorFactory.INSTANCE :
                salts.getOrDefault(config.getSalt(), EmptyGeneratorFactory.INSTANCE);
        return new CipherAlgorithmContext(config, codec, saltFactory.create(config), ivFactory.create(config));
    }
}
