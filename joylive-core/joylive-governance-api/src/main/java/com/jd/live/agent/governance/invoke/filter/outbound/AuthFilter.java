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
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * An outbound filter that injects authentication information into outbound requests based on the service policy.
 *
 * @see OutboundFilter
 * @see ServicePolicy
 * @see AuthPolicy
 * @see Authenticate
 */
@Injectable
@Extension(value = "AuthFilter", order = OutboundFilter.ORDER_AUTH)
public class AuthFilter implements OutboundFilter {

    @Inject
    private Map<String, Authenticate> authenticates;

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> filter(OutboundInvocation<R> invocation, E endpoint, OutboundFilterChain chain) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        AuthPolicy authPolicy = servicePolicy == null ? null : servicePolicy.getAuthPolicy();
        if (authPolicy != null && authPolicy.getType() != null) {
            Authenticate authenticate = authenticates.get(authPolicy.getType());
            if (authenticate != null) {
                authenticate.inject(invocation.getRequest(), authPolicy);
            }
        }
        return chain.filter(invocation, endpoint);
    }

}
