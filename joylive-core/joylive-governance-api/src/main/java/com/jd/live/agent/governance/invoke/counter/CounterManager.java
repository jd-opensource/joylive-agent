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
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.jd.live.agent.governance.policy.PolicyId.KEY_SERVICE_GROUP;
import static com.jd.live.agent.governance.policy.PolicyId.KEY_SERVICE_METHOD;

public class CounterManager {

    // TODO clean deleted endpoints
    private static final ConcurrentMap<String, ConcurrentMap<String, Counter>> METHOD_COUNTER =
            new ConcurrentHashMap<>();

    private static final CounterManager INSTANCE = new CounterManager();

    private CounterManager() {
    }

    public static CounterManager getInstance() {
        return INSTANCE;
    }

    public Counter getCounter(String endpoint, ServiceRequest request) {
        URI uri = URI.builder().host(request.getService()).path(request.getPath()).build()
                .parameters(KEY_SERVICE_GROUP, request.getGroup(), KEY_SERVICE_METHOD, request.getMethod());
        return getCounter(endpoint, uri.getUri());
    }

    public Counter getCounter(String endpoint, String uri) {
        return endpoint == null || uri == null
                ? null
                : METHOD_COUNTER.computeIfAbsent(endpoint, s -> new ConcurrentHashMap<>()).computeIfAbsent(uri, s -> new Counter());
    }

    public void removeCounter(String endpoint) {
        METHOD_COUNTER.remove(endpoint);
    }

    public void snapshot() {
        for (ConcurrentMap<String, Counter> map : METHOD_COUNTER.values()) {
            for (Counter counter : map.values()) {
                counter.snapshot();
            }
        }
    }

}
