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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
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
@ConditionalOnFlowControlEnabled
public class AuthFilter implements InboundFilter {

    @Inject
    private Map<String, Authenticate> authenticates;

    @Override
    public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        ServiceMetadata metadata = invocation.getServiceMetadata();
        ServicePolicy servicePolicy = metadata.getServicePolicy();
        if (servicePolicy != null && servicePolicy.authorized()) {
            AuthPolicy authPolicy = servicePolicy.getAuthPolicy(metadata.getConsumer());
            if (authPolicy == null) {
                return Futures.future(FaultType.UNAUTHORIZED.reject("the consumer is not authorized for service " + metadata.getServiceName()));
            }
            // check auth policy
            Authenticate authenticate = authenticates.get(authPolicy.getTypeOrDefault());
            if (authenticate != null) {
                Permission permission = authenticate.authenticate(invocation.getRequest(), authPolicy,
                        metadata.getServiceName(), metadata.getConsumer());
                if (permission != null && !permission.isSuccess()) {
                    return Futures.future(FaultType.UNAUTHORIZED.reject(permission.getMessage()));
                }
            }
        }
        return chain.filter(invocation);
    }

}
