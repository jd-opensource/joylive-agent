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
package com.jd.live.agent.governance.invoke.filter.outbound;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.fault.FaultInjection;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.fault.FaultInjectionPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * A filter that injects faults into the request processing based on the specified fault injection policy.
 *
 * @see OutboundFilter
 * @since 1.4.0
 */
@Injectable
@Extension(value = "FaultInjectionFilter", order = OutboundFilter.ORDER_FAULT_INJECTION)
public class FaultInjectionFilter implements OutboundFilter {

    @Inject
    private Map<String, FaultInjection> faultInjections;

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> filter(OutboundInvocation<R> invocation, E endpoint, OutboundFilterChain chain) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<FaultInjectionPolicy> faultPolicies = servicePolicy == null ? null : servicePolicy.getFaultInjectionPolicies();
        if (faultPolicies != null && !faultPolicies.isEmpty()) {
            for (FaultInjectionPolicy faultPolicy : faultPolicies) {
                if (faultPolicy.match(invocation)) {
                    FaultInjection injection = faultInjections.get(faultPolicy.getType());
                    if (injection != null) {
                        injection.acquire(faultPolicy);
                    }
                }
            }
        }
        return chain.filter(invocation, endpoint);

    }

}
