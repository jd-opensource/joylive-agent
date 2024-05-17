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
package com.jd.live.agent.core.util.time;

/**
 * Represents an object that has a timeout behavior, allowing for checks on its expiration and cancellation status.
 */
public interface Timeout {

    /**
     * Determines if the object has exceeded its timeout period.
     *
     * @return {@code true} if the object has expired; {@code false} otherwise.
     */
    boolean isExpired();

    /**
     * Checks if the operation associated with this object has been voluntarily cancelled.
     *
     * @return {@code true} if the operation has been cancelled; {@code false} otherwise.
     */
    boolean isCancelled();

    /**
     * Attempts to cancel the operation associated with this object.
     *
     * @return {@code true} if the operation was successfully cancelled; {@code false} if it could not be cancelled,
     * for example, because it has already been executed, expired, or previously cancelled.
     */
    boolean cancel();
}

