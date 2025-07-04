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
import com.jd.live.agent.implement.auth.jwt.JWTAlgorithmBuilder;
import com.jd.live.agent.governance.policy.service.auth.JWTAlgorithmContext;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

/**
 * Factory for creating HMAC algorithm instances using keys from a KeyStore.
 */
public abstract class HAMCAlgorithmBuilder implements JWTAlgorithmBuilder {

    @Override
    public Algorithm create(JWTAlgorithmContext context) throws Exception {
        String secretKey = context.getSecretKey();
        return isEmpty(secretKey) ? null : doCreate(secretKey);
    }

    protected abstract Algorithm doCreate(String secretKey) throws Exception;
}
