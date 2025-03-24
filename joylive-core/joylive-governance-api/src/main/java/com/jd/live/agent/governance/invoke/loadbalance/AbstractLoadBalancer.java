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
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;

import java.util.List;
import java.util.Random;

/**
 * Abstract implementation of the {@link LoadBalancer} interface.
 *
 * @since 1.0.0
 */
public abstract class AbstractLoadBalancer implements LoadBalancer {

    @Override
    public <T extends Endpoint> Candidate<T> elect(List<T> endpoints, LoadBalancePolicy policy, Invocation<?> invocation) {
        if (null == endpoints || endpoints.isEmpty()) {
            return null;
        } else if (endpoints.size() == 1) {
            return new Candidate<>(endpoints.get(0), 0);
        }
        random(endpoints, policy, invocation.getRandom());
        return doElect(endpoints, policy, invocation);
    }

    /**
     * Randomly filters the endpoints list based on the configured maximum number of candidates specified in the load balancing policy.
     * If the number of endpoints exceeds the maximum number of candidates, the list is filtered to retain only the specified number of candidates.
     * If the endpoints list is null or the maximum number of candidates is not configured, the list remains unchanged.
     *
     * @param <T>       The type of the endpoints, which must extend {@link Endpoint}.
     * @param endpoints The list of endpoints to filter. If {@code null}, it is treated as an empty list.
     * @param policy    The load balancing policy that defines the maximum number of candidates. If {@code null}, no filtering is applied.
     * @param random    A random number generator used for the filtering process.
     */
    protected <T extends Endpoint> void random(List<T> endpoints, LoadBalancePolicy policy, Random random) {
        int size = endpoints == null ? 0 : endpoints.size();
        Integer maxCandidates = policy == null ? null : policy.getMaxCandidates();
        if (maxCandidates != null && maxCandidates > 0 && size > maxCandidates) {
            // If a maximum number of candidates is configured, only the maxCandidates are taken.
            RouteTarget.filter(endpoints, maxCandidates, random);
        }
    }

    /**
     * Elects a candidate endpoint from the list based on the invocation.
     *
     * @param <T>        the type of the endpoint
     * @param endpoints  the list of endpoints to elect from
     * @param policy     The load balancing policy used to determine the selection strategy
     * @param invocation the invocation context
     * @return the elected candidate, or null if no candidate was elected
     */
    protected abstract <T extends Endpoint> Candidate<T> doElect(List<T> endpoints, LoadBalancePolicy policy, Invocation<?> invocation);
}

