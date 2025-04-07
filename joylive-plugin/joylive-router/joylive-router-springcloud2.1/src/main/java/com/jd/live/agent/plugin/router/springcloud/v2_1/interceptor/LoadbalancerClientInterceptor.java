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
package com.jd.live.agent.plugin.router.springcloud.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.ClientRequest;
import com.netflix.client.IResponse;
import com.netflix.client.config.IClientConfig;

/**
 * LoadbalancerClientInterceptor
 */
public class LoadbalancerClientInterceptor extends InterceptorAdaptor {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        AbstractLoadBalancerAwareClient target = (AbstractLoadBalancerAwareClient) mc.getTarget();
        try {
            // skip loadbalancer
            ClientRequest request = mc.getArgument(0);
            IClientConfig config = mc.getArgument(1);
            String serviceName = config != null ? config.getClientName() : target.getClientName();
            request = request.replaceUri(HttpUtils.newURI(request.getUri(), serviceName));
            IResponse response = target.execute(request, config);
            mc.skipWithResult(response);
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            mc.skipWithThrowable(cause instanceof ClientException ? cause : new ClientException(e));
        }
    }
}
