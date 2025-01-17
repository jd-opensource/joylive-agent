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
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.request.HeaderWriter.MultiValueMapWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;

/**
 * ClientHttpRequestFactoryInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Deprecated
// Duplicated transmission with sun http client
public class ClientHttpRequestFactoryInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public ClientHttpRequestFactoryInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        // for outbound traffic
        MethodContext mc = (MethodContext) ctx;
        ClientHttpRequest request = mc.getResult();
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(request.getHeaders());
        propagation.write(RequestContext.get(), new MultiValueMapWriter(headers));
        mc.setResult(request);
    }
}
