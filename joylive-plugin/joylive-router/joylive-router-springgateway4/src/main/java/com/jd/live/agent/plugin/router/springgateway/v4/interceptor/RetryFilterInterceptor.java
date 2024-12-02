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
package com.jd.live.agent.plugin.router.springgateway.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.plugin.router.springgateway.v4.config.GatewayConfig;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.web.server.ServerWebExchange;

import static com.jd.live.agent.core.util.type.ClassUtils.getValue;

/**
 * RetryFilterInterceptor
 *
 * @since 1.6.0
 */
public class RetryFilterInterceptor extends InterceptorAdaptor {

    private static final String FIELD_RETRY_CONFIG = "val$retryConfig";

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ServerWebExchange exchange = ctx.getArgument(0);
        GatewayFilterChain chain = ctx.getArgument(1);
        RetryGatewayFilterFactory.RetryConfig retryConfig = getValue(ctx.getTarget(), FIELD_RETRY_CONFIG);
        RequestContext.setAttribute(GatewayConfig.ATTRIBUTE_RETRY_CONFIG, retryConfig);
        mc.skipWithResult(chain.filter(exchange));

    }
}
