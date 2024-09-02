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
package com.jd.live.agent.governance.invoke.counter;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.policy.PolicyId;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that represents a counter for a specific endpoint.
 */
public class EndpointCounter {

    @Getter
    private final String name;

    private final ServiceCounter service;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private long accessTime;

    public EndpointCounter(String name, ServiceCounter service) {
        this.name = name;
        this.service = service;
        this.accessTime = System.currentTimeMillis();
    }

    /**
     * Returns the Counter instance associated with the specified URI, creating a new one if it doesn't
     * already exist.
     *
     * @param uri The URI for which to retrieve the Counter.
     * @return The Counter instance.
     */
    public Counter getOrCreate(URI uri) {
        return counters.computeIfAbsent(getMethodKey(uri), n -> new Counter(service));
    }

    /**
     * Takes a snapshot of all counters for this service.
     */
    protected void snapshot() {
        for (Counter counter : counters.values()) {
            counter.snapshot();
        }
    }

    private String getMethodKey(URI uri) {
        String method = uri.getParameter(PolicyId.KEY_SERVICE_METHOD);
        return method == null || method.isEmpty() ? uri.getPath() : uri.getPath() + "?method=" + method;
    }

}
