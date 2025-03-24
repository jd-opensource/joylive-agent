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
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;

import java.util.List;

/**
 * A adapter class for load balancing that forwards all its operations to another {@link LoadBalancer} instance.
 * This class acts as a wrapper or intermediary, allowing for additional behaviors to be inserted before or after
 * the delegation of the load balancing task. It implements the {@link LoadBalancer} interface and can be used
 * anywhere a LoadBalancer is required, providing a flexible mechanism for extending or modifying load balancing
 * behavior dynamically.
 *
 * @see LoadBalancer
 */
public class LoadBalancerAdapter implements LoadBalancer {

    /**
     * The {@link LoadBalancer} instance to which this delegate will forward all method calls.
     */
    protected LoadBalancer delegate;

    /**
     * Constructs a new {@code LoadBalancerDelegate} with a specified {@link LoadBalancer} to delegate to.
     *
     * @param delegate The {@link LoadBalancer} instance that this delegate will forward calls to.
     */
    public LoadBalancerAdapter(LoadBalancer delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T extends Endpoint> Candidate<T> elect(List<T> endpoints, LoadBalancePolicy policy, Invocation<?> invocation) {
        return delegate.elect(endpoints, policy, invocation);
    }
}
