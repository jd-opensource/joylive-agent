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
package com.jd.live.agent.governance.invoke.circuitbreak;

import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitLevel;
import com.jd.live.agent.governance.request.ServiceRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ComposeCircuitBreaker
 *
 * @since 1.1.0
 */
@Getter
public class ComposeCircuitBreaker extends AbstractCircuitBreaker {

    private final List<CircuitBreaker> circuitBreakers = new ArrayList<>();

    public <T extends ServiceRequest.OutboundRequest> ComposeCircuitBreaker(Map<String, CircuitBreakerFactory> factories,
                                                                            List<CircuitBreakerPolicy> policies,
                                                                            OutboundInvocation<T> invocation) {
        super(null);
        if (policies != null) {
            for (CircuitBreakerPolicy policy : policies) {
                CircuitBreakerFactory circuitBreakerFactory = factories.get(policy.getType());
                if (policy.getLevel() == CircuitLevel.INSTANCE) {
                    // TODO how to generate?
//                    policy.setId("");
                }
                CircuitBreaker circuitBreaker = circuitBreakerFactory.get(policy,
                        name -> invocation.getContext().getPolicySupplier().getPolicy().getService(name));
                if (circuitBreaker != null) {
                    circuitBreakers.add(circuitBreaker);
                }
            }
        }
    }

    /**
     * Try to get a permit return the result
     *
     * @return permission
     */
    @Override
    public boolean acquire() {
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            if (null != circuitBreaker && !circuitBreaker.acquire()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Records a failed call. This method must be invoked when a call failed.
     *
     * @param durationInMs The elapsed time duration of the call
     * @param throwable    The throwable which must be recorded
     */
    @Override
    public void onError(long durationInMs, Throwable throwable) {
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            if (null != circuitBreaker) {
                circuitBreaker.onError(durationInMs, throwable);
            }
        }
    }

    /**
     * Records a successful call. This method must be invoked when a call was
     * successful.
     *
     * @param durationInMs The elapsed time duration of the call
     */
    @Override
    public void onSuccess(long durationInMs) {
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            if (null != circuitBreaker) {
                circuitBreaker.onSuccess(durationInMs);
            }
        }
    }

}
