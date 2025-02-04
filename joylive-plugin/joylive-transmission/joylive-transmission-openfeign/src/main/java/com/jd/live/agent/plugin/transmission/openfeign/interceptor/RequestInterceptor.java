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
package com.jd.live.agent.plugin.transmission.openfeign.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.openfeign.request.FeignHeaderWriter;
import feign.Request;
import feign.RequestTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RequestInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public RequestInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    /**
     * {@inheritDoc}
     *
     * @see feign.Request#Request(Request.HttpMethod, String, Map, Request.Body, RequestTemplate)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] args = ctx.getArguments();
        HashMap<String, Collection<String>> modifiedHeaders = new HashMap<>(ctx.getArgument(2));
        propagation.write(RequestContext.get(), new FeignHeaderWriter(modifiedHeaders));
        args[2] = modifiedHeaders;
    }

}
