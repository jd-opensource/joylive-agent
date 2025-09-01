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

/**
 * Enumerates types of cryptographic generators with their size configuration.
 * Each type provides the appropriate size from cipher configuration.
 */
public enum CipherGeneratorType {
    /**
     * Salt generator type
     */
    SALT {
        @Override
        public int getSize(CipherConfig config) {
            return config.getSaltSize();
        }

        @Override
        public String getSeed(CipherConfig config) {
            return config.getSalt();
        }
    },

    /**
     * Initialization Vector (IV) generator type
     */
    IV {
        @Override
        public int getSize(CipherConfig config) {
            return config.getIvSize();
        }

        @Override
        public String getSeed(CipherConfig config) {
            return config.getIv();
        }
    };

    /**
     * Gets the size for this generator type from the configuration.
     *
     * @param config cipher configuration containing size parameters
     * @return configured size in bytes
     */
    public abstract int getSize(CipherConfig config);

    public abstract String getSeed(CipherConfig config);

}
