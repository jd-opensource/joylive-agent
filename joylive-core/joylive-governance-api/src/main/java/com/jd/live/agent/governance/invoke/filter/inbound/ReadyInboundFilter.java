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
package com.jd.live.agent.governance.invoke.filter.inbound;

import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.event.TrafficEvent.ActionType;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

/**
 * ReadyInboundFilter
 */
@Injectable
@Extension(value = "ReadyInboundFilter", order = InboundFilter.ORDER_INBOUND_LIVE_UNIT)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class ReadyInboundFilter implements InboundFilter {

    @Inject(Publisher.TRAFFIC)
    private Publisher<TrafficEvent> publisher;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Override
    public <T extends InboundRequest> void filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        if (!application.getStatus().inbound()) {
            invocation.publish(publisher, TrafficEvent.builder().actionType(ActionType.REJECT).requests(1));
            invocation.reject(FaultType.UNREADY, "Service instance is not ready,"
                    + " service=" + application.getService().getName()
                    + " address=" + Ipv4.getLocalIp());
        }
    }
}
