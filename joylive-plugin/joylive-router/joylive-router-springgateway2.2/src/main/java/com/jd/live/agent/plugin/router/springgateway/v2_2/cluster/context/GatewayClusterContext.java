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
package com.jd.live.agent.plugin.router.springgateway.v2_2.cluster.context;

import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.context.AbstractCloudClusterContext;
import lombok.Getter;

@Getter
public class GatewayClusterContext extends AbstractCloudClusterContext {

    private final Propagation propagation;

    public GatewayClusterContext(ServiceRegistryFactory registryFactory, Propagation propagation) {
        this.registryFactory = registryFactory;
        this.propagation = propagation;
    }

    @Override
    public boolean isRetryable() {
        return true;
    }

    @Override
    public RetryPolicy getDefaultRetryPolicy(String service) {
        // The RetryPolicy is provided by the Request.
        return null;
    }

}
