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
package com.jd.live.agent.plugin.transmission.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;
import com.jd.live.agent.governance.context.bag.Carrier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * WebHandlerDecoratorInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class WebHandlerDecoratorInterceptor extends InterceptorAdaptor {

    private final CargoRequire require;

    public WebHandlerDecoratorInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    /**
     * Enhanced logic before method execution
     *
     * @param ctx ExecutableContext
     * @see org.springframework.web.server.handler.FilteringWebHandler#handle(ServerWebExchange)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        ServerWebExchange exchange = (ServerWebExchange) ctx.getArguments()[0];
        HttpHeaders headers = exchange.getRequest().getHeaders();
        Carrier carrier = RequestContext.create();
        carrier.addCargo(require, HttpHeaders.writableHttpHeaders(headers));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ServerWebExchange exchange = (ServerWebExchange) ctx.getArguments()[0];
        HttpHeaders headers = exchange.getResponse().getHeaders();
        Mono<Void> mono = (Mono<Void>) mc.getResult();
        mono = mono.doFirst(() -> RequestContext.cargos(tag -> headers.addAll(tag.getKey(), tag.getValues())));
        mc.setResult(mono);
    }
}
