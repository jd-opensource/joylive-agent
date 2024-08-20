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
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.core.util.type.FieldList;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.plugin.router.springgateway.v4.config.GatewayConfig;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.web.server.ServerWebExchange;

/**
 * RetryGatewayFilterInterceptor
 *
 * @since 1.0.0
 */
public class RetryGatewayFilterInterceptor extends InterceptorAdaptor {

    private static final String FIELD_RETRY_CONFIG = "val$retryConfig";

    public RetryGatewayFilterInterceptor() {
    }

    /**
     * Enhanced logic before method execution
     * <p>
     *
     * @param ctx ExecutableContext
     * @see GatewayFilter#filter(ServerWebExchange, GatewayFilterChain)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        FieldList fieldList = ClassUtils.describe(ctx.getTarget().getClass()).getFieldList();
        FieldDesc fieldDesc = fieldList.getField(FIELD_RETRY_CONFIG);
        if (fieldDesc != null) {
            RetryConfig retryConfig = (RetryConfig) fieldDesc.get(ctx.getTarget());
            RequestContext.setAttribute(GatewayConfig.ATTRIBUTE_RETRY_CONFIG, retryConfig);
        }
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = mc.getArguments();
        ServerWebExchange exchange = (ServerWebExchange) arguments[0];
        GatewayFilterChain chain = (GatewayFilterChain) arguments[1];
        mc.setResult(chain.filter(exchange));
        mc.setSkip(true);
    }

}
