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
package com.jd.live.agent.implement.flowcontrol.resilience4j.circuitbreak;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerState;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateEvent;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateListener;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;

/**
 * InstanceCircuitBreakerStateListener
 *
 * @since 1.1.0
 */
public class InstanceCircuitBreakerStateListener implements CircuitBreakerStateListener {

    private static final Logger logger = LoggerFactory.getLogger(InstanceCircuitBreakerStateListener.class);

    private final CircuitBreakPolicy policy;

    private final String instanceId;

    public InstanceCircuitBreakerStateListener(CircuitBreakPolicy policy, String instanceId) {
        this.policy = policy;
        this.instanceId = instanceId;
    }

    @Override
    public void onStateChange(CircuitBreakerStateEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("[CircuitBreak]Instance state is transitioned from " + event.getFrom() + " to " + event.getTo() + ", uri=" + event.getUri());
        }
        if (event.getTo() == CircuitBreakerState.OPEN) {
            policy.addBroken(instanceId, System.currentTimeMillis() + policy.getWaitDurationInOpenState());
        } else if (event.getFrom() == CircuitBreakerState.OPEN) {
            policy.removeBroken(instanceId);
        }
    }
}
