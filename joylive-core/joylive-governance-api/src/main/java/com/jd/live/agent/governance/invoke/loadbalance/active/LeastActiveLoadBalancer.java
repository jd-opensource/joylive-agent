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
package com.jd.live.agent.governance.invoke.loadbalance.active;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.instance.counter.Counter;
import com.jd.live.agent.governance.instance.counter.CounterManager;
import com.jd.live.agent.governance.instance.counter.ServiceCounter;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.invoke.loadbalance.AbstractLoadBalancer;
import com.jd.live.agent.governance.invoke.loadbalance.Candidate;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.List;
import java.util.Random;

/**
 * A load balancer that selects the endpoint with the least active for an outbound request.
 * It is from org.apache.dubbo.rpc.cluster.loadbalance.ShortestResponseLoadBalance
 */
@Extension(value = LeastActiveLoadBalancer.LOAD_BALANCER_NAME, order = LoadBalancer.ORDER_LEAST_ACTIVE)
public class LeastActiveLoadBalancer extends AbstractLoadBalancer {

    /**
     * The name assigned to this load balancer.
     */
    public static final String LOAD_BALANCER_NAME = "LEAST_ACTIVE";

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends Endpoint> Candidate<T> doElect(List<T> endpoints, LoadBalancePolicy policy, Invocation<?> invocation) {
        ServiceRequest request = invocation.getRequest();
        int length = endpoints.size();
        long leastActive = Long.MAX_VALUE;
        int leastCount = 0;
        int[] leastIndexes = new int[length];
        Candidate<T>[] candidates = new Candidate[length];
        int totalWeight = 0;
        int firstWeight = 0;
        boolean sameWeight = true;

        URI uri = invocation.getServiceMetadata().getUri();
        CounterManager counterManager = invocation.getContext().getCounterManager();
        ServiceCounter serviceCounter = counterManager.getOrCreateCounter(request.getService(), request.getGroup());
        long accessTime = System.currentTimeMillis();
        T endpoint;
        Counter counter;
        int active;
        int weight;
        // Filter out all the least active endpoints
        for (int i = 0; i < length; i++) {
            endpoint = endpoints.get(i);
            counter = endpoint.getCounter(serviceCounter, uri, accessTime);
            active = counter.getActive();
            weight = endpoint.reweight(request);
            candidates[i] = new Candidate<>(endpoint, i, counter, weight);
            if (active < leastActive) {
                leastActive = active;
                leastCount = 1;
                leastIndexes[0] = i;
                totalWeight = weight;
                firstWeight = weight;
                sameWeight = true;
            } else if (active == leastActive) {
                leastIndexes[leastCount++] = i;
                totalWeight += weight;
                if (sameWeight && weight != firstWeight) {
                    sameWeight = false;
                }
            }
        }

        if (leastCount == 1) {
            return candidates[leastIndexes[0]];
        }
        int index;
        Random random = request.getRandom();
        if (!sameWeight && totalWeight > 0) {
            weight = random.nextInt(totalWeight);
            for (int i = 0; i < leastCount; i++) {
                index = leastIndexes[i];
                weight -= candidates[index].getWeight();
                if (weight < 0) {
                    return candidates[index];
                }
            }
        }
        index = random.nextInt(leastCount);
        return candidates[leastIndexes[index]];
    }

}

