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
package com.jd.live.agent.governance.counter;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a request that can be tracked for completion.
 * Allows monitoring and controlling the request lifecycle.
 */
public interface FlyingCounter {

    /**
     * Marks this request as completed.
     */
    void done();

    /**
     * Increments the reference count for this request.
     */
    void increment();

    /**
     * Decrements the reference count for this request.
     * @return The remaining references after decrement
     */
    int decrement();

    /**
     * Gets the current reference count.
     * @return The number of active references
     */
    int getCount();

    /**
     * Gets the completion future for this request.
     * @return A future that completes when the request is done
     */
    CompletableFuture<Boolean> waitDone();
}
