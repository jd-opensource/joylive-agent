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
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.invoke.counter.Counter;
import com.jd.live.agent.governance.invoke.counter.CounterManager;
import com.jd.live.agent.governance.invoke.counter.EndpointCounter;
import com.jd.live.agent.governance.invoke.counter.ServiceCounter;
import com.jd.live.agent.governance.invoke.loadbalance.AbstractLoadBalancer;
import com.jd.live.agent.governance.invoke.loadbalance.Candidate;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    @Override
    protected <T extends Endpoint> Candidate<T> doElect(List<T> endpoints, Invocation<?> invocation) {
        // Number of invokers
        int length = endpoints.size();
        // Estimated shortest response time of all invokers
        long shortestResponse = Long.MAX_VALUE;
        // The number of invokers having the same estimated shortest response time
        int shortestCount = 0;
        // The index of invokers having the same estimated shortest response time
        int[] shortestIndexes = new int[length];
        // the weight of every invoker
        int[] weights = new int[length];
        // The sum of the warmup weights of all the shortest response  invokers
        int totalWeight = 0;
        // The weight of the first shortest response invokers
        int firstWeight = 0;
        // Every shortest response invoker has the same weight value?
        boolean sameWeight = true;

        CounterManager counterManager = invocation.getContext().getCounterManager();
        ServiceRequest request = invocation.getRequest();
        URI uri = invocation.getServiceMetadata().getUri();
        ServiceCounter serviceCounter = counterManager.getOrCreate(uri);
        long accessTime = System.currentTimeMillis();
        // Filter out all the shortest response invokers
        for (int i = 0; i < length; i++) {
            T endpoint = endpoints.get(i);

            EndpointCounter endpointCounter = serviceCounter.getOrCreate(endpoint.getId());
            endpointCounter.setAccessTime(accessTime);
            Counter counter = endpointCounter.getOrCreate(uri);
            endpoint.setAttribute(Endpoint.ATTRIBUTE_COUNTER, counter);

            // Calculate the estimated response time from the product of active connections and succeeded average
            // elapsed time.
            long estimateResponse = counter.getSnapshot().getEstimateResponse();
            int weight = endpoint.getWeight(request);
            weights[i] = weight;
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
            return new Candidate<>(endpoints.get(shortestIndexes[0]), shortestIndexes[0]);
        }
        if (!sameWeight && totalWeight > 0) {
            int offsetWeight = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int i = 0; i < shortestCount; i++) {
                int shortestIndex = shortestIndexes[i];
                offsetWeight -= weights[shortestIndex];
                if (offsetWeight < 0) {
                    return new Candidate<>(endpoints.get(shortestIndex), shortestIndex);
                }
            }
        }
        int index = ThreadLocalRandom.current().nextInt(shortestCount);
        return new Candidate<>(endpoints.get(shortestIndexes[index]), shortestIndexes[index]);
    }

}

