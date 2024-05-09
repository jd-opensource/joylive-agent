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
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.retry.Retrier;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.plugin.router.springcloud.v3.request.FeignOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.FeignOutboundResponse;
import feign.Request;
import org.springframework.http.HttpStatus;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * FeignClientRequestInterceptor
 *
 * @since 1.0.0
 */
public class FeignClientRequestInterceptor extends AbstractHttpOutboundInterceptor<FeignOutboundRequest> {

    public FeignClientRequestInterceptor(InvocationContext context, List<OutboundFilter> filters) {
        super(context, filters);
    }

    /**
     * Enhanced logic before method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see feign.Client#execute(Request, Request.Options)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        if (RequestContext.getAttribute(Retrier.RETRY_MARK) != null) {
            return;
        } else {
            RequestContext.setAttribute(Retrier.RETRY_MARK, Boolean.TRUE);
        }
        MethodContext mc = (MethodContext) ctx;
        Request request = (Request) mc.getArguments()[0];
        HttpOutboundInvocation<FeignOutboundRequest> invocation;
        try {
            invocation = process(new FeignOutboundRequest(request, RequestContext.getAttribute(Carrier.ATTRIBUTE_SERVICE_ID)));
            Response response = invokeWithRetry(invocation, mc);
            if (response.getThrowable() != null) {
                if (response.getThrowable() instanceof InvocationTargetException) {
                    mc.setThrowable(((InvocationTargetException) response.getThrowable()).getTargetException());
                } else {
                    mc.setThrowable(response.getThrowable());
                }
            } else {
                mc.setResult(response.getResponse());
            }
        } catch (RejectException e) {
            mc.setResult(feign.Response.builder().status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value())
                    .body(e.getMessage().getBytes(StandardCharsets.UTF_8)).build());
        } finally {
            RequestContext.remove();
        }
        mc.setSkip(true);
    }

    @Override
    protected Response createResponse(Object result, Throwable throwable) {
        return new FeignOutboundResponse((feign.Response) result, throwable);
    }
}
