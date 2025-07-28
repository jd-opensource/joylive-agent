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
package com.jd.live.agent.plugin.transmission.servlet.javax.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.LockContext;
import com.jd.live.agent.core.extension.ExtensibleDesc;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.request.HeaderProviderFactory;
import com.jd.live.agent.governance.request.HeaderProviderRegistry;
import com.jd.live.agent.plugin.transmission.servlet.javax.request.HttpServletRequestParser;

import static com.jd.live.agent.plugin.transmission.servlet.javax.request.JavaxRequest.replace;

/**
 * An interceptor for HttpServlet requests to capture and restore context (cargo) from the request headers.
 * This is useful for maintaining request-specific context across different components of a distributed system.
 */
public class HttpServletInterceptor extends InterceptorAdaptor {

    private static final LockContext lock = new LockContext.DefaultLockContext();

    private final Propagation propagation;

    private final HeaderProviderRegistry registry;

    public HttpServletInterceptor(Propagation propagation, ExtensibleDesc<HeaderProviderFactory> extensibleDesc) {
        this.propagation = propagation;
        this.registry = new HeaderProviderRegistry(extensibleDesc.getExtensions());
    }

    /**
     * Enhanced logic before method execution
     *
     * @param ctx ExecutableContext
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        if (ctx.tryLock(lock)) {
            propagation.read(RequestContext.create(), new HttpServletRequestParser(replace(ctx.getArguments(), 0, registry)));
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        if (ctx.unlock()) {
            RequestContext.remove();
        }
    }
}
