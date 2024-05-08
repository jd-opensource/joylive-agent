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
import sun.net.www.MessageHeader;

/**
 * An interceptor that attaches additional information (a tag) to the HTTP request headers
 * before the request is executed. This is typically used for tracing, logging, or modifying
 * the request context programmatically.
 */
public class SunHttpClientInterceptor extends InterceptorAdaptor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        attachTag((MessageHeader) ctx.getArguments()[0]);
    }

    /**
     * Attaches a tag to the provided {@link MessageHeader} object. The actual tag is retrieved
     * from a {@link RequestContext} that presumably holds contextual information relevant to the
     * request being processed.
     *
     * @param header The {@link MessageHeader} to which the tag will be attached.
     */
    private void attachTag(MessageHeader header) {
        RequestContext.cargos(header::add);
    }
}

