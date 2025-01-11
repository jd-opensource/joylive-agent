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
package com.jd.live.agent.plugin.transmission.okhttp.v1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.LockContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.okhttp.v1.request.BuilderWriter;
import com.squareup.okhttp.Request.Builder;

/**
 * An interceptor that attaches additional metadata to each request.
 * This is typically used for adding tracing information, authentication tokens, or other request-scoped data that needs to
 * be included with every outbound HTTP request.
 */
public class OkHttpClientInterceptor extends InterceptorAdaptor {

    private static final LockContext lock = new LockContext.DefaultLockContext();

    private final Propagation propagation;

    public OkHttpClientInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (ctx.tryLock(lock)) {
            Builder builder = (Builder) ctx.getTarget();
            propagation.write(RequestContext.get(), BuilderWriter.of(builder));
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        ctx.unlock();
    }
}
