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
package com.jd.live.agent.core.config;

import com.jd.live.agent.core.inject.annotation.Configurable;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Configurable(prefix = "cipher", auto = true)
public class CipherConfig {

    public static final String COMPONENT_CIPHER_CONFIG = "cipherConfig";

    public static final String ENV_CIPHER_PASSWORD = "CONFIG_CIPHER_PASSWORD";

    public static final String CIPHER_RANDOM_ALGORITHM = "cipher.random.algorithm";

    public static final int DEFAULT_ITERATIONS = 1000;

    public static final int DEFAULT_SALT_SIZE = 8;

    public static final int DEFAULT_IV_SIZE = 16;

    private boolean enabled;

    private String cipher = "StandardPBE";

    private String algorithm = "PBEWithMD5AndDES";

    private String provider;

    private String password;

    private String codec = "base64";

    private String prefix = "ENC(";

    private String suffix = ")";

    private String saltType = "base64";

    private String salt;

    private int saltSize = DEFAULT_SALT_SIZE;

    private String ivType;

    private String iv;

    private int ivSize = DEFAULT_IV_SIZE;

    private int iterations = DEFAULT_ITERATIONS;

    private Map<String, String> properties;

    public String getProperty(String key) {
        return key == null || properties == null ? null : properties.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return key == null || properties == null ? defaultValue : properties.getOrDefault(key, defaultValue);
    }

    public int getSaltSize() {
        return saltSize <= 0 ? DEFAULT_SALT_SIZE : saltSize;
    }

    public int getIvSize() {
        return ivSize <= 0 ? DEFAULT_IV_SIZE : ivSize;
    }

    public int getIterations() {
        return iterations <= 0 ? DEFAULT_ITERATIONS : iterations;
    }
}

