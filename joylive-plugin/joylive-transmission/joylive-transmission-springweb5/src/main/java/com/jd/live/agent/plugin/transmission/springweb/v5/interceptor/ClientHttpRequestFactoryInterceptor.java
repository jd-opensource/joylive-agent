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
package com.jd.live.agent.plugin.transmission.springweb.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.net.URI;

/**
 * ClientHttpRequestFactoryInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ClientHttpRequestFactoryInterceptor extends InterceptorAdaptor {

    public ClientHttpRequestFactoryInterceptor() {
    }

    /**
     * Enhanced logic after method successfully executes. This method is called
     * after the target method completes successfully without throwing any exceptions.
     *
     * @param ctx ExecutableContext
     * @see org.springframework.http.client.ClientHttpRequestFactory#createRequest(URI, HttpMethod)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ClientHttpRequest request = (ClientHttpRequest) mc.getResult();
        RequestContext.cargos(tag -> request.getHeaders().addAll(tag.getKey(), tag.getValues()));
        mc.setResult(request);
    }
}
