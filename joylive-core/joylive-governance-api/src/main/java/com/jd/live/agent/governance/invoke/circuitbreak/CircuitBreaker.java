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

import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;

/**
 * CircuitBreaker
 *
 * @since 1.1.0
 */
public interface CircuitBreaker {

    /**
     * Try to get a permit return the result
     *
     * @return permission
     */
    default boolean acquire() {
        return true;
    }

    /**
     * Release the permission
     */
    default void release() {
        // do nothing
    }

    /**
     * Records a failed call. This method must be invoked when a call failed.
     *
     * @param durationInMs The elapsed time duration of the call
     * @param throwable    The throwable which must be recorded
     */
    void onError(long durationInMs, Throwable throwable);

    /**
     * Records a successful call. This method must be invoked when a call was successful.
     *
     * @param durationInMs The elapsed time duration of the call
     */
    void onSuccess(long durationInMs);

    /**
     * Register a listener to watch state change event.
     *
     * @param listener State change listener
     */
    void registerListener(CircuitBreakerStateListener listener);

    /**
     * Get circuit-breaker policy
     *
     * @return policy
     */
    CircuitBreakerPolicy getPolicy();

    /**
     * Get resource key
     *
     * @return Resource key
     */
    default String getResourceKey() {
        return null;
    }

    /**
     * Set resource key
     *
     * @param resourceKey Resource key
     */
    default void setResourceKey(String resourceKey) {
    }

}
