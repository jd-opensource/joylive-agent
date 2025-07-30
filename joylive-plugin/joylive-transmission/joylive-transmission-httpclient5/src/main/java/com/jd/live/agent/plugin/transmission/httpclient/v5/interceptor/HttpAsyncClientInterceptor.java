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
package com.jd.live.agent.plugin.transmission.httpclient.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.httpclient.v5.request.HttpMessageWriter;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpAsyncClientInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    private final Map<Class<?>, CacheObject<FieldAccessor>> accessors = new ConcurrentHashMap<>();

    public HttpAsyncClientInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        AsyncRequestProducer producer = ctx.getArgument(1);
        CacheObject<FieldAccessor> cache = accessors.computeIfAbsent(producer.getClass(),
                k -> CacheObject.of(FieldAccessorFactory.getAccessor(producer.getClass(), "request")));
        FieldAccessor accessor = cache.get();
        if (accessor != null) {
            Object request = accessor.get(producer);
            if (request instanceof HttpRequest) {
                propagation.write(RequestContext.get(), new HttpMessageWriter((HttpRequest) request));
            }
        }
    }

}
