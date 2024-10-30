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

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.auth.AllowResult;
import com.jd.live.agent.governance.policy.service.auth.PermissionPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * PermissionFilter
 *
 * @since 1.2.0
 */
@Injectable
@Extension(value = "PermissionFilter", order = InboundFilter.ORDER_PERMISSION)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class PermissionFilter implements InboundFilter {

    @Override
    public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<PermissionPolicy> policies = servicePolicy == null ? null : servicePolicy.getPermissionPolicies();
        if (null != policies && !policies.isEmpty()) {
            pass(invocation, policies);
        }
        return chain.filter(invocation);
    }

    /**
     * Passes the inbound invocation through the list of permission policies.
     *
     * @param invocation The inbound invocation to pass through the policies.
     * @param policies The list of permission policies to apply.
     */
    private <T extends InboundRequest> void pass(InboundInvocation<T> invocation, List<PermissionPolicy> policies) {
        boolean hasAllow = false;
        boolean allowed = false;
        boolean denied = false;
        for (PermissionPolicy policy : policies) {
            // match logic
            boolean passed = policy.match(invocation);
            if (policy.getType() == AllowResult.ALLOW) {
                hasAllow = true;
                if (passed) {
                    allowed = true;
                }
            } else if (policy.getType() == AllowResult.DENY && passed) {
                denied = true;
                break;
            }
        }
        // If denied, return false directly.
        if (denied) {
            invocation.reject(FaultType.PERMISSION_DENIED, "The traffic permission policy rejected the request.");
        }
        // If there is a allow rule list, but it has not passed any allow rule, return false.
        if (hasAllow && !allowed) {
            invocation.reject(FaultType.PERMISSION_DENIED, "The traffic permission policy rejected the request.");
        }
    }

}
