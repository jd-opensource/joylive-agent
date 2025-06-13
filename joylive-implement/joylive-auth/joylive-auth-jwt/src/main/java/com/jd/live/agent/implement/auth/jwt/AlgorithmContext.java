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
package com.jd.live.agent.implement.auth.jwt;

import com.jd.live.agent.governance.policy.service.auth.JWTPolicy;
import com.jd.live.agent.governance.security.KeyStore;
import lombok.Getter;

@Getter
public class AlgorithmContext {
    private final JWTPolicy policy;
    private final KeyStore keyStore;
    private final String consumer;
    private final String provider;
    private final String service;

    public AlgorithmContext(JWTPolicy policy, KeyStore keyStore, String consumer, String provider, String service) {
        this.policy = policy;
        this.keyStore = keyStore;
        this.consumer = consumer;
        this.provider = provider;
        this.service = service;
    }
}
