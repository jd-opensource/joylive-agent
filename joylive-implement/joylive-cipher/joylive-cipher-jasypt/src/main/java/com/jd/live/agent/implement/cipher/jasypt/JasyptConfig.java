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

import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import lombok.Getter;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.salt.SaltGenerator;

import java.util.Map;

import static com.jd.live.agent.governance.security.CipherAlgorithm.*;

@Getter
public class JasyptConfig {


    private final String algorithm;

    private final String password;

    private final int iterations;

    private final SaltGenerator saltGenerator;

    public JasyptConfig(Map<String, String> config) {
        Option option = MapOption.of(config);
        this.algorithm = getString(option, CIPHER_ALGORITHM, ENV_CIPHER_ALGORITHM, CIPHER_DEFAULT_ALGORITHM);
        this.password = getString(option, CIPHER_PASSWORD, CONFIG_CIPHER_PASSWORD, "");
        this.iterations = getPositive(option, CIPHER_ITERATIONS, CONFIG_CIPHER_ITERATIONS, CIPHER_DEFAULT_ITERATIONS);
        this.saltGenerator = getSaltGenerator(option);
    }

    private String getString(Option option, String key, String env, String defaultValue) {
        String value = option.getString(key);
        if (value == null || value.isEmpty()) {
            value = System.getenv(env);
        }
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    private int getPositive(Option option, String key, String env, int defaultValue) {
        String value = option.getString(key);
        if (value == null || value.isEmpty()) {
            value = System.getenv(env);
        }
        return Converts.getPositive(value, defaultValue);
    }

    private SaltGenerator getSaltGenerator(Option option) {
        return new RandomSaltGenerator();
    }
}
