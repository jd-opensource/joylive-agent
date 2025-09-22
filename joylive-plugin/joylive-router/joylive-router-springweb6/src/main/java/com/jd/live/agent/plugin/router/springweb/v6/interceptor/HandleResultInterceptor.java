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
package com.jd.live.agent.plugin.router.springweb.v6.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.plugin.router.springweb.v6.util.CloudUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.jd.live.agent.governance.util.ResponseUtils.labelHeaders;
import static com.jd.live.agent.plugin.router.springweb.v6.request.ReactiveInboundRequest.KEY_LIVE_EXCEPTION_HANDLED;
import static com.jd.live.agent.plugin.router.springweb.v6.request.ReactiveInboundRequest.KEY_LIVE_REQUEST;

/**
 * HandleResultInterceptor
 */
public class HandleResultInterceptor extends InterceptorAdaptor {

    private final ServiceConfig config;

    public HandleResultInterceptor(ServiceConfig config) {
        this.config = config;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        if (config.isResponseException()) {
            MethodContext mc = (MethodContext) ctx;
            ServerWebExchange exchange = (ServerWebExchange) mc.getArguments()[0];
            Boolean live = (Boolean) exchange.getAttributes().remove(KEY_LIVE_REQUEST);
            if (live != null && live) {
                Mono<Void> mono = mc.getResult();
                mono = mono.onErrorResume(ex -> {
                    exchange.getAttributes().put(KEY_LIVE_EXCEPTION_HANDLED, Boolean.TRUE);
                    HttpHeaders headers = CloudUtils.writable(exchange.getResponse().getHeaders());
                    labelHeaders(ex, headers::set);
                    return Mono.error(ex);
                });
                mc.setResult(mono);
            }
        }
    }
}
