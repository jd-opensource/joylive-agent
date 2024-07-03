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
package com.jd.live.agent.implement.flowcontrol.resilience4j.concurrencylimit;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.invoke.concurrencylimit.ConcurrencyLimiter;
import com.jd.live.agent.governance.invoke.concurrencylimit.ConcurrencyLimiterFactory;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;

/**
 * Resilience4jConcurrencyLimiterFactory
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "Resilience4j")
public class Resilience4jConcurrencyLimiterFactory implements ConcurrencyLimiterFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public ConcurrencyLimiter create(ConcurrencyLimitPolicy policy) {
        return new Resilience4jConcurrencyLimiter(policy);
    }
}
