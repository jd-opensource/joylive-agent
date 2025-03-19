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
package com.jd.live.agent.plugin.router.springcloud.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.InvocationContext;

/**
 * An abstract interceptor class for handling cloud cluster interactions.
 * This class provides a framework for intercepting and processing requests in a cloud environment,
 * with support for flow control and request routing.
 *
 * @param <T> the type of the request object
 * @since 1.7.0
 */
public abstract class AbstractCloudClusterInterceptor<T> extends InterceptorAdaptor {

    protected final InvocationContext context;

    public AbstractCloudClusterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (context.isFlowControlEnabled()) {
            request(ctx);
        } else {
            route(ctx);
        }
    }

    /**
     * Abstract method to handle the request processing logic.
     * Subclasses must implement this method to define specific request handling behavior.
     *
     * @param ctx the executable context of the intercepted method
     */
    protected abstract void request(ExecutableContext ctx);

    /**
     * Handles the routing logic for the request. This method is typically used for live and lane scenarios.
     * It retrieves the request object and service name, and sets them in the {@link RequestContext}.
     *
     * @param ctx the executable context of the intercepted method
     */
    protected void route(ExecutableContext ctx) {
        // only for live & lane
        T request = getRequest(ctx);
        String serviceName = getServiceName(request);
        RequestContext.setAttribute(Carrier.ATTRIBUTE_SERVICE_ID, serviceName);
        RequestContext.setAttribute(Carrier.ATTRIBUTE_REQUEST, request);
    }

    /**
     * Abstract method to retrieve the request object from the executable context.
     * Subclasses must implement this method to extract the request object.
     *
     * @param ctx the executable context of the intercepted method
     * @return the request object of type {@code T}
     */
    @SuppressWarnings("unchecked")
    protected T getRequest(ExecutableContext ctx) {
        return (T) ctx.getArgument(0);
    }

    /**
     * Abstract method to retrieve the service name from the request object.
     * Subclasses must implement this method to extract the service name.
     *
     * @param request the request object of type {@code T}
     * @return the service name associated with the request
     */
    protected abstract String getServiceName(T request);
}
