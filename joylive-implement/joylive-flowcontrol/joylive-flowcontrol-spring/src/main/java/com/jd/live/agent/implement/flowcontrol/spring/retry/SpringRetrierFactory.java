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
package com.jd.live.agent.implement.flowcontrol.spring.retry;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.invoke.retry.Retrier;
import com.jd.live.agent.governance.invoke.retry.RetrierFactory;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SpringRetrierFactory
 *
 * @since 1.0.0
 */
@Extension(value = "Spring")
public class SpringRetrierFactory implements RetrierFactory {

    private final Map<Long, AtomicReference<Retrier>> retriers = new ConcurrentHashMap<>();

    @Override
    public Retrier get(RetryPolicy policy) {
        if (policy == null) {
            return null;
        }
        AtomicReference<Retrier> reference = retriers.computeIfAbsent(policy.getId(), n -> new AtomicReference<>());
        Retrier retrier = reference.get();
        long version = retrier == null ? Long.MIN_VALUE : retrier.getPolicy().getVersion();
        if (version >= policy.getVersion()) {
            return retrier;
        }
        Retrier newRetrier = new SpringRetrier(policy);
        while (true) {
            retrier = reference.get();
            version = retrier == null ? Long.MIN_VALUE : retrier.getPolicy().getVersion();
            if (version >= policy.getVersion()) {
                return retrier;
            } else if (reference.compareAndSet(retrier, newRetrier)) {
                return newRetrier;
            }
        }
    }
}
