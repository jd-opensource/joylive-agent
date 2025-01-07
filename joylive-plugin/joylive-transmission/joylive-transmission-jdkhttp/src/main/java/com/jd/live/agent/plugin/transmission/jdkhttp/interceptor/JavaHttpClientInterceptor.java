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
import com.jd.live.agent.plugin.transmission.jdkhttp.request.BuilderParser;

/**
 * Interceptor for the Java HTTP Client's request builder implementation.
 * This class uses reflection to access and modify the headers of HTTP requests
 * constructed by the internal {@code HttpRequestBuilderImpl} class.
 *
 * <p>Due to module encapsulation in Java 9 and above, direct access to the internal
 * HTTP request builder is not possible. This interceptor bypasses these restrictions
 * to allow header manipulation for outgoing HTTP requests.</p>
 *
 * <p>Note: This interceptor is designed to work with specific versions of the JDK
 * where {@code HttpRequestBuilderImpl} and its method {@code setHeader} are present.</p>
 */
public class JavaHttpClientInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public JavaHttpClientInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        propagation.write(RequestContext.get(), BuilderParser.of(ctx.getTarget()));
    }
}
