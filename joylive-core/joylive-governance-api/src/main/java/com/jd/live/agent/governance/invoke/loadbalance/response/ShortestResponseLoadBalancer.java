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
import com.jd.live.agent.governance.instance.counter.Counter;
import com.jd.live.agent.governance.instance.counter.CounterManager;
import com.jd.live.agent.governance.instance.counter.ServiceCounter;
import com.jd.live.agent.governance.invoke.Invocation;
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

    @SuppressWarnings("unchecked")
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
        Candidate<T>[] candidates = new Candidate[length];
        // The sum of the warmup weights of all the shortest response  invokers
        int totalWeight = 0;
        // The weight of the first shortest response invokers
        int firstWeight = 0;
        // Every shortest response invoker has the same weight value?
        boolean sameWeight = true;

        ServiceRequest request = invocation.getRequest();
        URI uri = invocation.getServiceMetadata().getUri();
        CounterManager counterManager = invocation.getContext().getCounterManager();
        ServiceCounter serviceCounter = counterManager.getOrCreateCounter(request.getService(), request.getGroup());
        long accessTime = System.currentTimeMillis();
        T endpoint;
        Counter counter;
        long estimateResponse;
        int weight;
        // Filter out all the shortest response invokers
        for (int i = 0; i < length; i++) {
            endpoint = endpoints.get(i);
            counter = endpoint.getCounter(serviceCounter, uri, accessTime);

            // Calculate the estimated response time from the product of active connections and succeeded average
            // elapsed time.
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
        if (!sameWeight && totalWeight > 0) {
            weight = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int i = 0; i < shortestCount; i++) {
                index = shortestIndexes[i];
                weight -= candidates[index].getWeight();
                if (weight < 0) {
                    return candidates[index];
                }
            }
        }
        index = ThreadLocalRandom.current().nextInt(shortestCount);
        return candidates[shortestIndexes[index]];
    }

}

