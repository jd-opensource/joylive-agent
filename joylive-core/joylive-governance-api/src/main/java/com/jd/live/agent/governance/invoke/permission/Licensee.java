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
package com.jd.live.agent.governance.invoke.permission;

import com.jd.live.agent.governance.policy.PolicyVersion;

/**
 * Represents an entity that holds a license. License holders typically need to close
 *
 * @since 1.6.0
 */
public interface Licensee<P extends PolicyVersion> extends PolicyVersion, AutoCloseable {

    /**
     * Attempts to acquire a permit and returns the result.
     *
     * @return true if the permit is acquired successfully, false otherwise.
     */
    default boolean acquire() {
        return true;
    }

    /**
     * Retrieves the timestamp of the last successful acquisition.
     *
     * @return the timestamp of the last acquisition in milliseconds.
     */
    long getLastAccessTime();

    /**
     * Checks if the current time has exceeded the specified timeout period since the last acquire time.
     *
     * @param timeout the timeout period in milliseconds
     * @return true if the resource has expired, false otherwise
     */
    default boolean isExpired(long timeout) {
        return System.currentTimeMillis() - getLastAccessTime() > timeout;
    }

    /**
     * Retrieves the policy that governs the behavior of the circuit breaker.
     *
     * @return the circuit breaker policy.
     */
    P getPolicy();

    /**
     * Exchanges the current policy of the circuit breaker with the specified policy.
     *
     * @param policy the new policy to be set for the circuit breaker
     */
    void exchange(P policy);

    @Override
    default long getVersion() {
        return getPolicy().getVersion();
    }

    @Override
    default void close() {

    }
}
