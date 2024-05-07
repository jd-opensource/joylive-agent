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
package com.jd.live.agent.plugin.router.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractHttpOutboundInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.retry.RetrierFactory;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.plugin.router.springcloud.v3.request.ReactiveOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.ClientHttpOutboundResponse;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * InterceptingClientHttpRequestInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class InterceptingClientHttpRequestInterceptor extends AbstractHttpOutboundInterceptor<ReactiveOutboundRequest> {

    public InterceptingClientHttpRequestInterceptor(InvocationContext context, List<OutboundFilter> filters, Map<String, RetrierFactory> retrierFactories) {
        super(context, filters, retrierFactories);
    }

    /**
     * Enhanced logic before method execution<br>
     * <p>
     * see org.springframework.http.client.InterceptingClientHttpRequest#executeInternal(HttpHeaders)
     *
     * @param ctx ExecutableContext
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        HttpRequest request = (HttpRequest) mc.getTarget();
        OutboundInvocation.HttpOutboundInvocation<ReactiveOutboundRequest> outboundInvocation = null;
        try {
            outboundInvocation = process(new ReactiveOutboundRequest(request, RequestContext.getAttribute(Carrier.ATTRIBUTE_SERVICE_ID)));
        } catch (RejectException e) {
            mc.setThrowable(HttpClientErrorException.create(
                    e.getMessage(),
                    HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE,
                    HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.getReasonPhrase(),
                    request.getHeaders(),
                    null,
                    StandardCharsets.UTF_8));
        }
        mc.setResult(invokeWithRetry(outboundInvocation, mc));
        mc.setSkip(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onExit(ExecutableContext ctx) {
        RequestContext.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final Supplier<Response> createRetrySupplier(Object target, Method method, Object[] allArguments, Object result) {
        return () -> {
            Response response = null;
            method.setAccessible(true);
            try {
                Object r = method.invoke(target, allArguments);
                response = new ClientHttpOutboundResponse((ClientHttpResponse) r, null);
            } catch (IllegalAccessException ignored) {
                // ignored
            } catch (Throwable throwable) {
                response = new ClientHttpOutboundResponse((ClientHttpResponse) result, throwable);
            }
            return response;
        };
    }
}
