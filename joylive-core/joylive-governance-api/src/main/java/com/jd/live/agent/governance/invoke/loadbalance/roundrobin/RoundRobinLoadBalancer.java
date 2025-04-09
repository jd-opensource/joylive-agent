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
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
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

    private static final Function<Long, AtomicLong> POLICY_COUNTER_FUNC = s -> new AtomicLong(0L);

    private static final Function<String, AtomicLong> SERVICE_COUNTER_FUNC = s -> new AtomicLong(0L);

    private final Map<Long, AtomicLong> policyCounters = new ConcurrentHashMap<>();

    private final Map<String, AtomicLong> serviceCounters = new ConcurrentHashMap<>();

    private final AtomicLong globalCounter = new AtomicLong(0);

    @Override
    public <T extends Endpoint> Candidate<T> doElect(List<T> endpoints, LoadBalancePolicy policy, Invocation<?> invocation) {
        AtomicLong counter = globalCounter;
        ServiceMetadata metadata = invocation.getServiceMetadata();
        ServicePolicy servicePolicy = metadata.getServicePolicy();
        LoadBalancePolicy loadBalancePolicy = servicePolicy == null ? null : servicePolicy.getLoadBalancePolicy();
        String uniqueName = loadBalancePolicy != null ? null : metadata.getUniqueName();
        if (loadBalancePolicy != null) {
            counter = policyCounters.computeIfAbsent(loadBalancePolicy.getId(), POLICY_COUNTER_FUNC);
        } else if (uniqueName != null && !uniqueName.isEmpty()) {
            counter = serviceCounters.computeIfAbsent(uniqueName, SERVICE_COUNTER_FUNC);
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

