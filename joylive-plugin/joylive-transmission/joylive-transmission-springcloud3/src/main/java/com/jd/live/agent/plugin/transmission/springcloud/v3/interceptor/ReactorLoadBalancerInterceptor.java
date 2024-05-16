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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClientBuilderInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Deprecated
public class ReactorLoadBalancerInterceptor extends InterceptorAdaptor {

    public ReactorLoadBalancerInterceptor() {
    }

    /**
     * Enhanced logic before method execution
     *
     * @param ctx ExecutableContext
     * @see WebClient.Builder#build()
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        ClientRequest request = (ClientRequest) ctx.getArguments()[0];
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(request.headers());
        attachTag(headers);
    }

    private void attachTag(HttpHeaders headers) {
        RequestContext.cargos(tag -> headers.addAll(tag.getKey(), tag.getValues()));
    }


}
