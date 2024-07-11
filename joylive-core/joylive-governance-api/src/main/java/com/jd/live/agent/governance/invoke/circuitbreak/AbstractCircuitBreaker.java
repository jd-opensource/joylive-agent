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

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import lombok.Getter;

/**
 * AbstractCircuitBreaker
 *
 * @since 1.1.0
 */
@Getter
public abstract class AbstractCircuitBreaker implements CircuitBreaker {

    private final CircuitBreakPolicy policy;

    private final URI uri;

    private long lastAcquireTime;

    public AbstractCircuitBreaker(CircuitBreakPolicy policy, URI uri) {
        this.policy = policy;
        this.uri = uri;
    }

    @Override
    public boolean acquire() {
        lastAcquireTime = System.currentTimeMillis();
        return doAcquire();
    }

    /**
     * Performs the actual acquisition logic.
     * Subclasses must implement this method to define the specific acquisition behavior.
     *
     * @return true if the acquisition is successful, false otherwise.
     */
    protected abstract boolean doAcquire();

}
