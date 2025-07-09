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
package com.jd.live.agent.governance.counter.internal;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.counter.Counter;
import com.jd.live.agent.governance.counter.EndpointCounter;
import com.jd.live.agent.governance.counter.ServiceCounter;
import com.jd.live.agent.governance.policy.PolicyId;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that represents a counter for a specific endpoint.
 */
public class InternalEndpointCounter implements EndpointCounter {

    @Getter
    private final String name;

    @Getter
    private final ServiceCounter parent;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private long accessTime;

    public InternalEndpointCounter(String name, ServiceCounter parent) {
        this.name = name;
        this.parent = parent;
        this.accessTime = System.currentTimeMillis();
    }

    @Override
    public Counter getOrCreateCounter(URI uri) {
        return counters.computeIfAbsent(getKey(uri), n -> new InternalCounter(this));
    }

    /**
     * Takes a snapshot of all counters for this service.
     */
    protected void snapshot() {
        for (Counter counter : counters.values()) {
            counter.snapshot();
        }
    }

    private String getKey(URI uri) {
        String method = uri.getParameter(PolicyId.KEY_SERVICE_METHOD);
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "";
        } else {
            int pos = path.length() - 1;
            while (pos >= 0 && path.charAt(pos) == '/') {
                pos--;
            }
            path = pos < 0 ? "" : path.substring(0, pos + 1);
        }
        return method == null || method.isEmpty() ? path : path + "?method=" + method;
    }

}
