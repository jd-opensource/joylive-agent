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
package com.jd.live.agent.governance.invoke.loadbalance;

import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.Invocation;

import java.util.List;

/**
 * Abstract implementation of the {@link LoadBalancer} interface.
 * This class provides a base implementation for the elect method and requires subclasses to implement the {@link #doElect(List, Invocation)} method.
 *
 * @since 1.0.0
 */
public abstract class AbstractLoadBalancer implements LoadBalancer {

    @Override
    public <T extends Endpoint> Candidate<T> elect(List<T> endpoints, Invocation<?> invocation) {
        if (null == endpoints || endpoints.isEmpty()) {
            return null;
        } else if (endpoints.size() == 1) {
            return new Candidate<>(endpoints.get(0), 0);
        }
        return doElect(endpoints, invocation);
    }

    /**
     * Elects a candidate endpoint from the list based on the invocation.
     *
     * @param <T>        the type of the endpoint
     * @param endpoints  the list of endpoints to elect from
     * @param invocation the invocation context
     * @return the elected candidate, or null if no candidate was elected
     */
    protected abstract <T extends Endpoint> Candidate<T> doElect(List<T> endpoints, Invocation<?> invocation);
}

