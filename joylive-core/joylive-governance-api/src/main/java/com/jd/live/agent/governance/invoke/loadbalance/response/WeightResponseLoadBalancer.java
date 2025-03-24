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
import com.jd.live.agent.governance.instance.counter.CounterSnapshot;
import com.jd.live.agent.governance.instance.counter.ServiceCounter;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.invoke.loadbalance.AbstractLoadBalancer;
import com.jd.live.agent.governance.invoke.loadbalance.Candidate;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.invoke.loadbalance.randomweight.RandomWeight;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.request.ServiceRequest;
import lombok.Getter;

import java.util.List;
import java.util.Random;

/**
 * A load balancer implementation that selects endpoints based on their weighted response times.
 * This load balancer calculates the average response time of endpoints and assigns weights
 * accordingly to improve load distribution and performance.
 */
@Extension(value = WeightResponseLoadBalancer.LOAD_BALANCER_NAME, order = LoadBalancer.ORDER_WEIGHT_RESPONSE)
public class WeightResponseLoadBalancer extends AbstractLoadBalancer {

    /**
     * The name assigned to this load balancer.
     */
    public static final String LOAD_BALANCER_NAME = "WEIGHT_RESPONSE";

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends Endpoint> Candidate<T> doElect(List<T> endpoints, LoadBalancePolicy policy, Invocation<?> invocation) {
        ServiceRequest request = invocation.getRequest();
        Random random = request.getRandom();
        random(endpoints, policy, random);
        URI uri = invocation.getServiceMetadata().getUri();
        CounterManager counterManager = invocation.getContext().getCounterManager();
        ServiceCounter serviceCounter = counterManager.getOrCreateCounter(request.getService(), request.getGroup());
        long accessTime = System.currentTimeMillis();

        long response = 0;
        int length = endpoints.size();
        ReponseCandidate<T>[] candidates = new ReponseCandidate[length];
        ReponseCandidate<T> candidate;
        T endpoint;
        Counter counter;
        CounterSnapshot snapshot;
        int success = 0;
        for (int i = 0; i < length; i++) {
            endpoint = endpoints.get(i);
            counter = endpoint.getCounter(serviceCounter, uri, accessTime);
            snapshot = counter.getSnapshot();
            candidate = new ReponseCandidate<>(endpoint, i, counter, snapshot, endpoint.reweight(request));
            candidates[i] = candidate;
            if (snapshot.getSucceeded() > 0) {
                success++;
                response += candidate.getResponse();
            }
        }

        if (success > 0 && response > 0) {
            double avg = Math.ceil(response * 1.0 / success);
            for (int i = 0; i < length; i++) {
                candidates[i].reweight(avg);
            }
        }
        return RandomWeight.elect(candidates, random);
    }

    @Getter
    private static class ReponseCandidate<T extends Endpoint> extends Candidate<T> {

        private final CounterSnapshot snapshot;

        private final long response;

        ReponseCandidate(T target, int index, Counter counter, CounterSnapshot snapshot, Integer weight) {
            super(target, index, counter, weight);
            this.snapshot = snapshot;
            this.response = snapshot.getSucceededAverageElapsed();
        }

        public void reweight(double avgResponse) {
            this.weight = response > 0 ? (int) Math.ceil(weight * avgResponse / response) : weight;
        }
    }
}

