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
package com.jd.live.agent.plugin.transmission.jdkhttp.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.jdkhttp.request.MessageHeaderWriter;
import sun.net.www.MessageHeader;

/**
 * An interceptor that attaches additional information (a tag) to the HTTP request headers
 * before the request is executed. This is typically used for tracing, logging, or modifying
 * the request context programmatically.
 */
public class SunHttpClientInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public SunHttpClientInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MessageHeader header = ctx.getArgument(0);
        propagation.write(RequestContext.get(), new MessageHeaderWriter(header));
    }
}

