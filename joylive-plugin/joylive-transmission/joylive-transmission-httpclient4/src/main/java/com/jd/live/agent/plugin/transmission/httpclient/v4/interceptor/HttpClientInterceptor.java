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
package com.jd.live.agent.plugin.transmission.httpclient.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpRequestBase;

import static com.jd.live.agent.governance.request.header.HeaderParser.StringHeaderParser.writer;

public class HttpClientInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public HttpClientInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object request = ctx.getArguments()[1];
        Carrier carrier = RequestContext.getOrCreate();
        if (request instanceof HttpRequestBase) {
            HttpRequestBase httpRequestBase = (HttpRequestBase) request;
            propagation.write(carrier, writer(httpRequestBase::addHeader));
        } else if (request instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) request;
            propagation.write(carrier, writer(httpRequest::addHeader));
        }
    }
}
