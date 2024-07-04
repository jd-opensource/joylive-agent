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
package com.jd.live.agent.governance.invoke.filter.route;

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * A virtual filter that extends the list of instances for an outbound invocation if the number of instances is below a
 * certain threshold. This filter is part of the governance configuration and is applied in the virtual routing phase.
 *
 * <p>This filter is enabled when flow control is enabled and the virtual governance feature is enabled.</p>
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "VirtualFilter", order = OutboundFilter.ORDER_VIRTUAL)
@ConditionalOnProperty(GovernanceConfig.CONFIG_VIRTUAL_ENABLED)
public class VirtualFilter implements OutboundFilter {

    /**
     * The maximum size of the instance list. If the number of instances is below this size, the list will be extended.
     * The default value is 500.
     */
    @Config("agent.governance.router.virtual")
    private int size = 500;

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain) {
        List<? extends Endpoint> instances = invocation.getInstances();
        if (size > 0 && instances != null && !instances.isEmpty() && instances.size() < size) {
            List<Endpoint> result = new ArrayList<>(size);
            result.addAll(instances);
            int remain = size - instances.size();
            int count = result.size();
            while (remain > 0 && remain >= count) {
                result.addAll(result.subList(0, count));
                remain -= count;
                count = result.size();
            }
            if (remain > 0) {
                result.addAll(result.subList(0, remain));
            }
            invocation.setInstances(result);
        }
        chain.filter(invocation);
    }
}
