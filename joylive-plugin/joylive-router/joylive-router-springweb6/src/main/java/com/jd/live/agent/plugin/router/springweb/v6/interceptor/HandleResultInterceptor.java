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
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.jd.live.agent.core.util.ExceptionUtils.exceptionHeaders;
import static com.jd.live.agent.plugin.router.springweb.v6.request.ReactiveInboundRequest.KEY_LIVE_REQUEST;

/**
 * HandleResultInterceptor
 */
public class HandleResultInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ServerWebExchange exchange = (ServerWebExchange) mc.getArguments()[0];
        Boolean live = (Boolean) exchange.getAttributes().get(KEY_LIVE_REQUEST);
        if (live != null && live) {
            Mono<Void> mono = mc.getResult();
            mono = mono.onErrorResume(ex -> {
                exchange.getAttributes().put(KEY_LIVE_REQUEST, Boolean.TRUE);
                HttpHeaders headers = exchange.getResponse().getHeaders();
                exceptionHeaders(ex, headers::set);
                return Mono.error(ex);
            });
            mc.setResult(mono);
        }
    }
}
