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

import lombok.Getter;

@Getter
public class CipherAlgorithmContext {

    private final CipherConfig config;

    private final StringCodec codec;

    private final CipherGenerator salt;

    private final CipherGenerator iv;

    public CipherAlgorithmContext(CipherConfig config, StringCodec codec, CipherGenerator salt) {
        this(config, codec, salt, null);
    }

    public CipherAlgorithmContext(CipherConfig config, StringCodec codec, CipherGenerator salt, CipherGenerator iv) {
        this.config = config;
        this.codec = codec;
        this.salt = salt;
        this.iv = iv;
    }
}
