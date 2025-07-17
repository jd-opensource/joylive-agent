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

import java.util.Map;

/**
 * Factory interface for creating cryptographic cipher instances.
 */
public interface CipherFactory {

    String COMPONENT_CIPHER_FACTORY = "CipherFactory";

    /**
     * Creates a new cipher instance configured with the specified parameters.
     *
     * @param config the configuration map containing cipher parameters
     *               (e.g., "algorithm", "mode", "padding")
     * @return initialized and configured cipher instance
     */
    Cipher create(CipherConfig config);

    /**
     * Creates a cipher instance based on configuration.
     *
     * @param factories Available cipher factories (keyed by cipher type)
     * @param config    Cipher configuration
     * @return Configured cipher instance:
     */
    static Cipher create(Map<String, CipherFactory> factories, CipherConfig config) {
        String cipherType = config.getCipher();
        CipherFactory factory = !config.isEnabled() || cipherType == null ? null : factories.get(cipherType);
        return factory == null ? null : factory.create(config);
    }
}
