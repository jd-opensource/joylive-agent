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
package com.jd.live.agent.implement.auth.jwt.hamc;

import com.auth0.jwt.algorithms.Algorithm;
import com.jd.live.agent.implement.auth.jwt.AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.AlgorithmContext;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

/**
 * Factory for creating HMAC algorithm instances using keys from a KeyStore.
 */
public abstract class HAMCAlgorithmBuilder implements AlgorithmBuilder {

    @Override
    public Algorithm create(AlgorithmContext context) throws Exception {
        String secret = context.getPolicy().getSecret();
        return isEmpty(secret) ? null : doCreate(secret);
    }

    protected abstract Algorithm doCreate(String secret) throws Exception;
}
