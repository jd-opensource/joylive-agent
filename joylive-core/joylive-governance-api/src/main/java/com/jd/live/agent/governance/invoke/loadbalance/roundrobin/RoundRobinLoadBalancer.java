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
package com.jd.live.agent.governance.invoke.loadbalance.roundrobin;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.invoke.loadbalance.AbstractLoadBalancer;
import com.jd.live.agent.governance.invoke.loadbalance.Candidate;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * RoundRobinLoadBalancer is an implementation of the {@link LoadBalancer} interface that
 * provides a simple round-robin load balancing strategy. It iterates through a list of
 * available endpoints and selects the next one in sequence for handling a request.
 * If a service policy with a specific load balance policy is associated with the request,
 * the round-robin counter is scoped to that policy, ensuring that balancing is consistent
 * within the context of the policy.
 */
@Extension(value = RoundRobinLoadBalancer.LOAD_BALANCER_NAME, order = LoadBalancer.ORDER_ROUND_ROBIN)
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {

    /**
     * The name assigned to this load balancer.
     */
    public static final String LOAD_BALANCER_NAME = "ROUND_ROBIN";

    /**
     * A function that creates a new {@code AtomicLong} instance set to 0L.
     */
    private static final Function<Long, AtomicLong> COUNTER_FUNC = s -> new AtomicLong(0L);

    /**
     * A map of counters, keyed by load balance policy IDs, for maintaining the round-robin state
     * specific to a load balance policy.
     */
    private final Map<Long, AtomicLong> counters = new ConcurrentHashMap<>();

    /**
     * A global counter for the round-robin load balancing algorithm.
     */
    private final AtomicLong global = new AtomicLong(0);

    @Override
    public <T extends Endpoint> Candidate<T> doElect(List<T> endpoints, LoadBalancePolicy policy, Invocation<?> invocation) {
        AtomicLong counter = global;
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        LoadBalancePolicy loadBalancePolicy = servicePolicy == null ? null : servicePolicy.getLoadBalancePolicy();
        if (loadBalancePolicy != null) {
            counter = counters.computeIfAbsent(loadBalancePolicy.getId(), COUNTER_FUNC);
        }
        long count = counter.getAndIncrement();
        if (count < 0) {
            counter.set(0);
            count = counter.getAndIncrement();
        }
        // Ensure the index is within the bounds of the endpoints list.
        int index = (int) (count % endpoints.size());
        return new Candidate<>(endpoints.get(index), index);
    }

    @Override
    protected <T extends Endpoint> void random(List<T> endpoints, LoadBalancePolicy policy, Random random) {
        // This method is not used in the RoundRobinLoadBalancer.
    }
}

