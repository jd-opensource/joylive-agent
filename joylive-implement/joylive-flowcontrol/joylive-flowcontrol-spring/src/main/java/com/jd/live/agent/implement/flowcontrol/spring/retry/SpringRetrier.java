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

import com.jd.live.agent.governance.invoke.retry.Retrier;
import com.jd.live.agent.governance.policy.service.retry.RetryPolicy;
import com.jd.live.agent.governance.response.Response;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.function.Supplier;

/**
 * SpringRetrier
 *
 * @since 1.0.0
 */
public class SpringRetrier implements Retrier {

    private final RetryPolicy policy;

    private final RetryTemplate retryTemplate;

    public SpringRetrier(RetryPolicy policy) {
        this.policy = policy;
        this.retryTemplate = new RetryTemplate();
        SpringRetryPolicy retryPolicy = new SpringRetryPolicy(policy);
        retryPolicy.setMaxAttempts(policy.getRetry() + 1);
        retryTemplate.setRetryPolicy(retryPolicy);
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(policy.getRetryInterval());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Response> T execute(Supplier<T> supplier) {
        // TODO retry timeout
        return retryTemplate.execute(context -> {
            T response = supplier.get();
            context.setAttribute(SpringRetryPolicy.RESPONSE_KEY, response);
            return response;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetryPolicy getPolicy() {
        return policy;
    }
}
