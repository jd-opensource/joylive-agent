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
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.auth.AuthResult;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * AuthFilter
 *
 * @since 1.2.0
 */
@Injectable
@Extension(value = "AuthFilter", order = InboundFilter.ORDER_AUTH)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class AuthFilter implements InboundFilter {

    @Inject
    private Map<String, Authenticate> authenticates;

    @Override
    public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        AuthPolicy authPolicy = servicePolicy == null ? null : servicePolicy.getAuthPolicy();
        if (authPolicy != null && authPolicy.getType() != null) {
            Authenticate authenticate = authenticates.get(authPolicy.getType());
            if (authenticate != null) {
                AuthResult result = authenticate.authenticate(invocation.getRequest(), authPolicy);
                if (result != null && !result.isSuccess()) {
                    if (result.getError() != null && !result.getError().isEmpty()) {
                        invocation.reject(FaultType.UNAUTHORIZED, "The traffic auth policy rejected the request. caused by " + result.getError());
                    } else {
                        invocation.reject(FaultType.UNAUTHORIZED, "The traffic auth policy rejected the request.");
                    }
                }
            }
        }
        return chain.filter(invocation);
    }

}
