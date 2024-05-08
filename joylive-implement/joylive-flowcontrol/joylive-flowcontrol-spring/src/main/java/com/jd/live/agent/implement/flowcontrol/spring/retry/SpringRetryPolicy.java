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

import com.jd.live.agent.governance.policy.service.retry.RetryPolicy;
import com.jd.live.agent.governance.response.Response;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;

/**
 * SpringRetryPolicy
 *
 * @since 1.0.0
 */
public class SpringRetryPolicy extends SimpleRetryPolicy {

    public static final String RESPONSE_KEY = "response";

    public static final String DEADLINE_KEY = "deadline";

    private final RetryPolicy retryPolicy;

    public SpringRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable t = context.getLastThrowable();
        if (context.hasAttribute(SpringRetryPolicy.DEADLINE_KEY)) {
            Long deadline = (Long) context.getAttribute(SpringRetryPolicy.DEADLINE_KEY);
            if (System.currentTimeMillis() > deadline) {
                return false;
            }
        }
        boolean can = (t == null || retryPolicy.isRetry(t)) && context.getRetryCount() < this.getMaxAttempts();
        if (!can && context.hasAttribute(RESPONSE_KEY)) {
            Response response = (Response) context.getAttribute(RESPONSE_KEY);
            can = retryPolicy.isRetry(response.getCode()) && context.getRetryCount() < this.getMaxAttempts();
        }
        if (!can) {
            can = super.canRetry(context);
        }
        return can;
    }
}
