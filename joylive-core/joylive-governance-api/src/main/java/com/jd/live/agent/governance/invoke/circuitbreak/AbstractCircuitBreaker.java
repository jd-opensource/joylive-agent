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
import com.jd.live.agent.governance.invoke.permission.AbstractLicensee;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakInfo;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPhase;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractCircuitBreaker
 *
 * @since 1.1.0
 */
public abstract class AbstractCircuitBreaker extends AbstractLicensee<CircuitBreakPolicy> implements CircuitBreaker {

    @Getter
    protected final URI uri;

    protected final AtomicReference<CircuitBreakerStateWindow> windowRef = new AtomicReference<>();

    public AbstractCircuitBreaker(CircuitBreakPolicy policy, URI uri) {
        this.policy = policy;
        this.uri = uri;
    }

    @Override
    public boolean acquire() {
        if (!started.get()) {
            return true;
        }
        lastAccessTime = System.currentTimeMillis();
        return doAcquire();
    }

    @Override
    public boolean acquireWhen(int instances) {
        return getPolicy().isProtectMode(instances) || acquire();
    }

    @Override
    public void release() {
        if (started.get()) {
            doRelease();
        }
    }

    @Override
    public CircuitBreakInfo getInfo(long now) {
        CircuitBreakerStateWindow state = windowRef.get();
        if (state == null) {
            return null;
        }
        switch (state.getState()) {
            case OPEN:
                return now <= state.getEndTime() ? new CircuitBreakInfo(CircuitBreakPhase.OPEN) : new CircuitBreakInfo(CircuitBreakPhase.HALF_OPEN);
            case HALF_OPEN:
                return new CircuitBreakInfo(CircuitBreakPhase.HALF_OPEN);
            case CLOSED:
                if (!policy.isRecoveryEnabled()) {
                    return new CircuitBreakInfo(CircuitBreakPhase.CLOSED);
                } else if (now > state.getEndTime()) {
                    windowRef.compareAndSet(state, null);
                    return new CircuitBreakInfo(CircuitBreakPhase.CLOSED);
                }
                return new CircuitBreakInfo(CircuitBreakPhase.RECOVER, policy.getRecoveryRatio(now - state.getStartTime()));
            case DISABLED:
            default:
                return null;
        }
    }

    @Override
    public void onError(long durationInMs, Throwable throwable) {
        if (started.get()) {
            doOnError(durationInMs, throwable);
        }
    }

    @Override
    public void onSuccess(long durationInMs) {
        if (started.get()) {
            doOnSuccess(durationInMs);
        }
    }

    /**
     * Performs the actual acquisition logic.
     * Subclasses must implement this method to define the specific acquisition behavior.
     *
     * @return true if the acquisition is successful, false otherwise.
     */
    protected abstract boolean doAcquire();

    /**
     * Releases the acquired permit.
     */
    protected abstract void doRelease();

    /**
     * Records a failed call. This method should be invoked when a call fails.
     *
     * @param durationInMs The elapsed time duration of the call in milliseconds.
     * @param throwable    The throwable that represents the failure.
     */
    protected abstract void doOnError(long durationInMs, Throwable throwable);

    /**
     * Records a successful call. This method should be invoked when a call is successful.
     *
     * @param durationInMs The elapsed time duration of the call in milliseconds.
     */
    protected abstract void doOnSuccess(long durationInMs);

    @Override
    protected void doExchange(CircuitBreakPolicy older, CircuitBreakPolicy newer) {
        newer.exchange(older);
    }

}
