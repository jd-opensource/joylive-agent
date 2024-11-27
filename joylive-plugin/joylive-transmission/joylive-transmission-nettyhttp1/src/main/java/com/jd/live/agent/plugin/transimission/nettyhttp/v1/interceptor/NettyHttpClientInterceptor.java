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
package com.jd.live.agent.plugin.transimission.nettyhttp.v1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import io.netty.handler.codec.http.HttpMethod;
import reactor.netty.http.client.HttpClient;

/**
 * NettyHttpClientInterceptor
 */
@Deprecated
public class NettyHttpClientInterceptor extends InterceptorAdaptor {


    /**
     * Enhanced logic after method successfully execute
     *
     * @param ctx ExecutableContext
     * @see reactor.netty.http.client.HttpClient#request(HttpMethod)
     */
    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        HttpClient client = mc.getResult();
        if (RequestContext.hasCargo()) {
            HttpClient newClient = client.headers(headers -> RequestContext.cargos(cargo -> headers.set(cargo.getKey(), cargo.getValue())));
            if (client.getClass().isAssignableFrom(newClient.getClass())) {
                // fix netty reactor 0.9.20.RELEASE
                mc.setResult(newClient);
            }
        }
    }
}
