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

import com.jd.live.agent.governance.context.RequestContext;
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

    private final RetryPolicy policy;

    public SpringRetryPolicy(RetryPolicy policy) {
        super(policy.getRetry() + 1);
        this.policy = policy;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Response response = (Response) context.getAttribute(RESPONSE_KEY);
        if (response == null) {
            return false;
        } else if (RequestContext.isTimeout()) {
            return false;
        } else if (context.getRetryCount() >= getMaxAttempts()) {
            return false;
        } else if (policy.isRetry(response.getCode())) {
            return true;
        } else if (policy.isRetry(response.getThrowable())) {
            return true;
        } else {
            return response.isRetryable();
        }
    }
}
