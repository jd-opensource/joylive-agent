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
package com.jd.live.agent.plugin.transmission.springweb.v7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.request.HeaderWriter.MultiValueMapWriter;
import com.jd.live.agent.plugin.transmission.springweb.v7.util.HeaderUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * DefaultExchangeFunctionInterceptor
 */
public class DefaultExchangeFunctionInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public DefaultExchangeFunctionInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // for outbound traffic
        ClientRequest request = (ClientRequest) ctx.getArguments()[0];
        MultiValueMap<String, String> headers = HeaderUtils.writeableHeaders(request.headers());
        propagation.write(RequestContext.get(), new MultiValueMapWriter(headers));
    }
}
