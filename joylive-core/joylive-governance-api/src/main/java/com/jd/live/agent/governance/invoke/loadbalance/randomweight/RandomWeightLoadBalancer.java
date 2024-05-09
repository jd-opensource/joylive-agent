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
package com.jd.live.agent.governance.invoke.loadbalance.randomweight;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.List;

/**
 * RandomWeightLoadBalancer is an implementation of the {@link LoadBalancer} interface that
 * provides a random selection strategy for choosing an endpoint from a list of available
 * endpoints. Each endpoint is associated with a weight, and the selection is influenced
 * by these weights to ensure that endpoints with higher weights are more likely to be chosen.
 *
 * @since 1.0.0
 */
@Extension(value = RandomWeightLoadBalancer.LOAD_BALANCER_NAME, order = LoadBalancer.ORDER_RANDOM_WEIGHT)
public class RandomWeightLoadBalancer implements LoadBalancer {

    /**
     * The name assigned to this load balancer.
     */
    public static final String LOAD_BALANCER_NAME = "RANDOM";

    /**
     * A singleton instance of the RandomWeightLoadBalancer.
     */
    public static final LoadBalancer INSTANCE = new RandomWeightLoadBalancer();

    @Override
    public <T extends Endpoint> T doSelect(List<T> endpoints, Invocation<?> invocation) {
        ServiceRequest request = invocation.getRequest();
        // Use the RandomWeight utility to select an endpoint based on the weights.
        return RandomWeight.select(endpoints, e -> e.getWeight(request));
    }
}

