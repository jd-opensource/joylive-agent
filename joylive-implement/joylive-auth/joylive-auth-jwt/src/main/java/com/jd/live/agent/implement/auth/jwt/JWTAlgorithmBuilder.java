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

import com.auth0.jwt.algorithms.Algorithm;
import com.jd.live.agent.governance.policy.service.auth.JWTAlgorithmContext;

/**
 * Builder interface for JWT signing algorithms.
 * <p>
 * Provides a standard way to construct Algorithm instances for JWT signing operations.
 * Implementations handle specific algorithm types and their configuration requirements.
 */
public interface JWTAlgorithmBuilder {
    /**
     * Creates a configured Algorithm instance for JWT signing/verification.
     *
     * @param context The configuration context containing parameters and keys
     * @return Initialized Algorithm ready for use
     * @throws Exception if algorithm creation fails (e.g., invalid parameters,
     *                   unsupported configuration, or key material issues)
     */
    Algorithm create(JWTAlgorithmContext context) throws Exception;
}
