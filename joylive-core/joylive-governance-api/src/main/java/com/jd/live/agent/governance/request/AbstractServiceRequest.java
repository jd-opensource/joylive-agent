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
package com.jd.live.agent.governance.request;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides an abstract base class for service requests.*
 * @param <T> The type of the original request object this class wraps.
 */
@Getter
public abstract class AbstractServiceRequest<T> extends AbstractAttributes implements ServiceRequest {

    /**
     * The original request object associated with this service request.
     */
    protected final T request;

    /**
     * A set of identifiers representing attempts made in the context of this service request.
     * This is useful for tracking retries or duplicate handling.
     */
    protected Set<String> attempts;

    /**
     * The timestamp when the service request was created.
     */
    protected long startTime;

    /**
     * Constructs an instance of {@code AbstractServiceRequest} with the original request object.
     *
     * <p>This constructor initializes the {@code request} field and sets the {@code startTime}
     * to the current system time.
     *
     * @param request The original request object. Cannot be null.
     * @throws IllegalArgumentException if the request is null.
     */
    public AbstractServiceRequest(T request) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Adds an attempt identifier to the set of attempts.
     *
     * <p>This method is used to record an attempt made in the context of this service request.
     * If the {@code attempts} set is null, it initializes the set before adding the attempt.
     *
     * @param attempt The identifier of the attempt to be added. If null, the method does nothing.
     */
    public void addAttempt(String attempt) {
        if (attempt != null) {
            if (attempts == null) {
                attempts = new HashSet<>();
            }
            attempts.add(attempt);
        }
    }
}

