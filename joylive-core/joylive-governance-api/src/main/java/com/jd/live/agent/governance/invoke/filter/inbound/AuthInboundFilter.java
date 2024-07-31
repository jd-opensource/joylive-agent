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
import com.jd.live.agent.governance.policy.service.auth.AuthType;
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
            boolean hasWhiteList = false;
            boolean passedWhiteList = false;
            boolean blockedByBlackList = false;
            for (AuthPolicy policy : policies) {
                // match logic
                boolean policyResult = policy.match(invocation);
                if (policy.getType() == AuthType.WHITE) {
                    hasWhiteList = true;
                    if (policyResult) {
                        passedWhiteList = true;
                    }
                } else if (policy.getType() == AuthType.BLACK && policyResult) {
                    blockedByBlackList = true;
                    break;
                }
            }
            // If blocked by the blacklist, return false directly.
            if (blockedByBlackList) {
                invocation.reject(FaultType.AUTHENTICATED, "The traffic authentication policy rejects the request.");
            }
            // If there is a whitelist, but it has not passed any whitelist, return false.
            if (hasWhiteList && !passedWhiteList) {
                invocation.reject(FaultType.AUTHENTICATED, "The traffic authentication policy rejects the request.");
            }
        }
        chain.filter(invocation);
    }

}
