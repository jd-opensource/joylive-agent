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
package com.jd.live.agent.plugin.router.springgateway.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.handler.FilteringWebHandler;

/**
 * GatewayInterceptor
 *
 * @since 1.0.0
 */
public class GatewayInterceptor extends InterceptorAdaptor {

    /**
     * Enhanced logic before method execution
     * <p>
     *
     * @param ctx ExecutableContext
     * @see FilteringWebHandler#handle(ServerWebExchange)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        RequestContext.setAttribute(Carrier.ATTRIBUTE_GATEWAY, Boolean.TRUE);
    }
}
