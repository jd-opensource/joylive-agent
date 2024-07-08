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
package com.jd.live.agent.governance.invoke.concurrencylimit;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;

/**
 * ConcurrencyLimiterFactory
 *
 * @since 1.0.0
 */
@Extensible("ConcurrencyLimiterFactory")
public interface ConcurrencyLimiterFactory {

    /**
     * Retrieves a concurrency limiter for the given concurrency limit policy. If a concurrency limiter for the policy
     * already exists and its version is greater than or equal to the policy version, it is returned.
     * Otherwise, a new concurrency limiter is created using the {@link #create(ConcurrencyLimitPolicy)} method.
     *
     * @param policy The concurrency limit policy for which to retrieve or create a concurrency limiter.
     * @return A concurrency limiter that corresponds to the given policy, or null if the policy is null.
     */
    ConcurrencyLimiter get(ConcurrencyLimitPolicy policy);

    /**
     * Retrieves a new instance of a {@link ConcurrencyLimiter} based on the provided
     * concurrency limit policy.
     *
     * @param policy the policy that defines the concurrency limiting rules.
     * @return a new instance of a concurrency limiter configured according to the policy.
     */
    ConcurrencyLimiter create(ConcurrencyLimitPolicy policy);

}
