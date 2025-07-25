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
package com.jd.live.agent.governance.invoke.loadbalance.response;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.counter.Counter;
import com.jd.live.agent.governance.counter.CounterManager;
import com.jd.live.agent.governance.counter.ServiceCounter;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.invoke.loadbalance.AbstractLoadBalancer;
import com.jd.live.agent.governance.invoke.loadbalance.Candidate;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.List;
import java.util.Random;

/**
 * A load balancer that selects the endpoint with the shortest response time for an outbound request.
 * It is from org.apache.dubbo.rpc.cluster.loadbalance.ShortestResponseLoadBalance
 */
@Extension(value = ShortestResponseLoadBalancer.LOAD_BALANCER_NAME, order = LoadBalancer.ORDER_SHORTEST_RESPONSE)
public class ShortestResponseLoadBalancer extends AbstractLoadBalancer {

    /**
     * The name assigned to this load balancer.
     */
    public static final String LOAD_BALANCER_NAME = "SHORTEST_RESPONSE";

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends Endpoint> Candidate<T> doElect(List<T> endpoints, LoadBalancePolicy policy, Invocation<?> invocation) {
        ServiceRequest request = invocation.getRequest();
        int length = endpoints.size();
        long shortestResponse = Long.MAX_VALUE;
        int shortestCount = 0;
        int[] shortestIndexes = new int[length];
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
        long estimateResponse;
        int weight;
        // Filter out all the shortest response endpoints
        for (int i = 0; i < length; i++) {
            endpoint = endpoints.get(i);
            counter = endpoint.getCounter(serviceCounter, uri, accessTime);

            estimateResponse = counter.getSnapshot().getEstimateResponse();
            weight = endpoint.reweight(request);
            candidates[i] = new Candidate<>(endpoint, i, counter, weight);
            // Same as LeastActiveLoadBalance
            if (estimateResponse < shortestResponse) {
                shortestResponse = estimateResponse;
                shortestCount = 1;
                shortestIndexes[0] = i;
                totalWeight = weight;
                firstWeight = weight;
                sameWeight = true;
            } else if (estimateResponse == shortestResponse) {
                shortestIndexes[shortestCount++] = i;
                totalWeight += weight;
                if (sameWeight && i > 0 && weight != firstWeight) {
                    sameWeight = false;
                }
            }
        }

        if (shortestCount == 1) {
            return candidates[shortestIndexes[0]];
        }
        int index;
        Random random = request.getRandom();
        if (!sameWeight && totalWeight > 0) {
            weight = random.nextInt(totalWeight);
            for (int i = 0; i < shortestCount; i++) {
                index = shortestIndexes[i];
                weight -= candidates[index].getWeight();
                if (weight < 0) {
                    return candidates[index];
                }
            }
        }
        index = random.nextInt(shortestCount);
        return candidates[shortestIndexes[index]];
    }

}

