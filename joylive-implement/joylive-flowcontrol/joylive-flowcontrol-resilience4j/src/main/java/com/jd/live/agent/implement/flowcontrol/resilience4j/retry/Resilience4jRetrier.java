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
package com.jd.live.agent.implement.flowcontrol.resilience4j.retry;

import com.jd.live.agent.governance.invoke.retry.Retrier;
import com.jd.live.agent.governance.policy.service.retry.RetryPolicy;
import com.jd.live.agent.governance.response.Response;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Resilience4jRetrier
 *
 * @since 1.0.0
 */
public class Resilience4jRetrier implements Retrier {

    private final RetryPolicy policy;

    private final Retry retry;

    public Resilience4jRetrier(RetryPolicy policy) {
        this.policy = policy;
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(policy.getRetry() + 1)
                .waitDuration(Duration.ofMillis(policy.getRetryInterval()))
                .retryOnResult(response -> policy.isRetry(((Response) response).getCode()))
                .retryOnException(policy::isRetry)
                .failAfterMaxAttempts(true)
                .build();
        RetryRegistry registry = RetryRegistry.of(config);
        retry = registry.retry(policy.getId().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Response> T execute(Supplier<T> supplier) {
        // TODO retry timeout
        return retry.executeSupplier(supplier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetryPolicy getPolicy() {
        return policy;
    }
}
