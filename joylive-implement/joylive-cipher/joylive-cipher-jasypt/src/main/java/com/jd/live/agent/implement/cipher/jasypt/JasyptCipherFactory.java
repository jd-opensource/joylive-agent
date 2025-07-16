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
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.governance.security.*;
import com.jd.live.agent.governance.security.base64.Base64StringCodec;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.salt.SaltGenerator;
import org.jasypt.salt.StringFixedSaltGenerator;

import java.util.HashMap;
import java.util.Map;

import static com.jd.live.agent.governance.security.CipherAlgorithm.*;

/**
 * {@link CipherFactory} implementation using Jasypt library.
 */
@Injectable
@Extension("jasypt")
public class JasyptCipherFactory implements CipherFactory {

    private static final Map<String, CipherAlgorithmFactory<JasyptConfig>> factories = new HashMap<>();

    static {
        register(new JasyptPBECipherAlgorithmFactory());
    }

    @Inject
    private Map<String, StringCodec> codecs;

    @Override
    public Cipher create(Map<String, String> config) {
        String algorithm = config == null ? null : config.get(CIPHER_ALGORITHM);
        algorithm = algorithm == null || algorithm.isEmpty() ? CIPHER_DEFAULT_ALGORITHM : algorithm;
        CipherAlgorithmFactory<JasyptConfig> factory = factories.get(algorithm);
        CipherAlgorithm ca = factory == null ? null : factory.create(createConfig(config));
        return ca == null ? null : new JasyptCipher(ca);
    }

    /**
     * Builds Jasypt configuration from input options.
     *
     * @param config Raw configuration input (can be null)
     * @return Fully configured JasyptConfig instance
     */
    private JasyptConfig createConfig(Map<String, String> config) {
        Option option = MapOption.of(config);
        return JasyptConfig.builder()
                .algorithm(getString(option, CIPHER_ALGORITHM, ENV_CIPHER_ALGORITHM, CIPHER_DEFAULT_ALGORITHM))
                .password(getString(option, CIPHER_PASSWORD, CONFIG_CIPHER_PASSWORD, ""))
                .iterations(getPositive(option, CIPHER_ITERATIONS, CONFIG_CIPHER_ITERATIONS, CIPHER_DEFAULT_ITERATIONS))
                .saltGenerator(getSaltGenerator(option))
                .codec(getCodec(option))
                .build();
    }

    /**
     * Gets a configuration string with fallback logic:
     */
    private String getString(Option option, String key, String env, String defaultValue) {
        String value = option.getString(key);
        if (value == null || value.isEmpty()) {
            value = System.getenv(env);
        }
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    /**
     * Gets a positive integer configuration with same fallback logic as getString().
     * Ensures returned value is always positive (uses default if conversion fails).
     */
    private int getPositive(Option option, String key, String env, int defaultValue) {
        String value = option.getString(key);
        if (value == null || value.isEmpty()) {
            value = System.getenv(env);
        }
        return Converts.getPositive(value, defaultValue);
    }

    /**
     * Creates salt generator - fixed salt if configured, random salt otherwise.
     */
    private SaltGenerator getSaltGenerator(Option option) {
        String salt = option.getString(CIPHER_SALT);
        return salt != null && !salt.isEmpty() ? new StringFixedSaltGenerator(salt) : new RandomSaltGenerator();
    }

    /**
     * Gets the string codec implementation by name.
     * Falls back to Base64 if specified codec isn't found.
     */
    private StringCodec getCodec(Option option) {
        String type = getString(option, CIPHER_CODEC, ENV_CIPHER_CODEC, Base64StringCodec.name);
        StringCodec result = codecs == null ? null : codecs.get(type);
        return result == null ? Base64StringCodec.INSTANCE : result;
    }

    private static void register(CipherAlgorithmFactory<JasyptConfig> factory) {
        for (String name : factory.getNames()) {
            factories.put(name, factory);
        }
    }
}
