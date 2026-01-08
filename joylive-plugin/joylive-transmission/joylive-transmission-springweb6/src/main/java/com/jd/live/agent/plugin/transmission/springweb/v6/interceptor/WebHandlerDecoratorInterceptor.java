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
package com.jd.live.agent.plugin.transmission.springweb.v6.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.request.HeaderReader.MultiValueMapReader;
import com.jd.live.agent.plugin.transmission.springweb.v6.util.CloudUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

/**
 * WebHandlerDecoratorInterceptor
 */
public class WebHandlerDecoratorInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public WebHandlerDecoratorInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // for inbound traffic
        ServerWebExchange exchange = ctx.getArgument(0);
        HttpHeaders headers = CloudUtils.writable(exchange.getRequest().getHeaders());
        propagation.read(RequestContext.create(), new MultiValueMapReader(headers));
    }
}
