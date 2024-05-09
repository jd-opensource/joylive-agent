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

import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.invoke.retry.Retrier;
import com.jd.live.agent.governance.policy.service.retry.RetryPolicy;
import com.jd.live.agent.governance.response.Response;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Implements a retrier using the Resilience4j library to apply retry logic
 * based on a specified {@link RetryPolicy}. This class allows operations to be
 * retried according to configurable criteria such as the maximum number of attempts,
 * wait duration between attempts, and conditions that trigger a retry.
 *
 * @since 1.0.0
 */
public class Resilience4jRetrier implements Retrier {

    private final RetryPolicy policy;

    private final Retry retry;

    /**
     * Constructs a new {@code Resilience4jRetrier} with the specified retry policy.
     * This sets up the retry behavior based on the policy parameters such as maximum
     * retry attempts, retry interval, and retry conditions.
     *
     * @param policy The {@link RetryPolicy} defining the retry behavior.
     */
    public Resilience4jRetrier(RetryPolicy policy) {
        this.policy = policy;
        this.retry = getRetry(policy);
    }

    /**
     * Configures and returns a {@link Retry} instance based on the provided
     * {@link RetryPolicy}. This method sets up the retry configuration including
     * the maximum number of attempts, wait duration between retries, and conditions
     * that dictate whether a retry should occur.
     *
     * @param policy The {@link RetryPolicy} to base the retry configuration on.
     * @return A configured {@link Retry} instance.
     */
    private Retry getRetry(RetryPolicy policy) {
        RetryConfig config = RetryConfig.<Response>custom()
                .maxAttempts(policy.getRetry() + 1)
                .waitDuration(Duration.ofMillis(policy.getRetryInterval()))
                .retryOnResult(this::predicate)
                .failAfterMaxAttempts(true)
                .build();
        return RetryRegistry.of(config).retry(policy.getId().toString());
    }

    /**
     * Evaluates whether a given response should trigger a retry. This method
     * checks various conditions such as null responses, timeout occurrences,
     * specific response codes, throwable conditions, and custom predicates
     * defined within the response to determine if a retry is warranted.
     *
     * @param response The {@link Response} to evaluate for retry.
     * @return {@code true} if the response meets the criteria for retrying; {@code false} otherwise.
     */
    private boolean predicate(Response response) {
        if (response == null) {
            return false;
        } else if (RequestContext.isTimeout()) {
            return false;
        } else if (policy.isRetry(response.getCode())) {
            return true;
        } else if (policy.isRetry(response.getThrowable())) {
            return true;
        } else {
            return response.isRetryable();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Response> T execute(Supplier<T> supplier) {
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
