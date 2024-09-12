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

import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.policy.service.auth.AuthResult;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.List;

/**
 * AuthInboundFilter
 *
 * @since 1.2.0
 */
@Injectable
@Extension(value = "AuthInboundFilter", order = InboundFilter.ORDER_INBOUND_AUTH)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class AuthInboundFilter implements InboundFilter, ExtensionInitializer {

    @Override
    public void initialize() {
    }

    @Override
    public <T extends InboundRequest> void filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<AuthPolicy> policies = servicePolicy == null ? null : servicePolicy.getAuthPolicies();
        if (null != policies && !policies.isEmpty()) {
            boolean hasAllow = false;
            boolean allowed = false;
            boolean denied = false;
            for (AuthPolicy policy : policies) {
                // match logic
                boolean passed = policy.match(invocation);
                if (policy.getType() == AuthResult.ALLOW) {
                    hasAllow = true;
                    if (passed) {
                        allowed = true;
                    }
                } else if (policy.getType() == AuthResult.DENY && passed) {
                    denied = true;
                    break;
                }
            }
            // If denied, return false directly.
            if (denied) {
                invocation.reject(FaultType.UNAUTHORIZED, "The traffic authentication policy rejected the request.");
            }
            // If there is a allow rule list, but it has not passed any allow rule, return false.
            if (hasAllow && !allowed) {
                invocation.reject(FaultType.UNAUTHORIZED, "The traffic authentication policy rejected the request.");
            }
        }
        chain.filter(invocation);
    }

}
