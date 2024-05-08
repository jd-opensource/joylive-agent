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
package com.jd.live.agent.governance.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.InboundInvocation.GatewayInboundInvocation;
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.filter.*;
import com.jd.live.agent.governance.invoke.retry.Retrier;
import com.jd.live.agent.governance.invoke.retry.RetrierFactory;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.retry.RetryPolicy;
import com.jd.live.agent.governance.request.HttpRequest.HttpInboundRequest;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.Response;

import java.util.List;
import java.util.function.Supplier;

/**
 * AbstractInterceptor is the base class for all interceptors within the framework.
 * It provides a common context for the interception process and defines the structure
 * for inbound, outbound, and route interceptors.
 */
public abstract class AbstractInterceptor extends InterceptorAdaptor {

    /**
     * The InvocationContext that holds the state of the current invocation.
     */
    protected final InvocationContext context;

    /**
     * Constructs a new AbstractInterceptor with the given InvocationContext.
     *
     * @param context the InvocationContext for this interceptor
     */
    public AbstractInterceptor(InvocationContext context) {
        this.context = context;
    }

    /**
     * The abstract base class for inbound interceptors.
     * Inbound interceptors are responsible for processing incoming requests before they are dispatched.
     */
    public static abstract class AbstractInboundInterceptor<R extends InboundRequest, I extends InboundInvocation<R>> extends AbstractInterceptor {

        /**
         * An array of inbound filters to be applied to the inbound request.
         */
        protected final InboundFilter[] inboundFilters;

        /**
         * Constructs a new AbstractInboundInterceptor with the given InvocationContext and inbound filters.
         *
         * @param context the InvocationContext for this interceptor
         * @param filters the list of inbound filters to be applied
         */
        public AbstractInboundInterceptor(InvocationContext context, List<InboundFilter> filters) {
            super(context);
            this.inboundFilters = filters == null ? new InboundFilter[0] : filters.toArray(new InboundFilter[0]);
        }

        /**
         * Processes the inbound request by creating an InboundInvocation and applying the inbound filters.
         *
         * @param request the inbound request to be processed
         * @return the resulting InboundInvocation after processing
         */
        protected I process(R request) {
            I invocation = createInlet(request);
            process(invocation);
            return invocation;
        }

        /**
         * Creates a new InboundInvocation for the given inbound request.
         *
         * @param request the inbound request for which to create an InboundInvocation
         * @return a new InboundInvocation instance
         */
        protected abstract I createInlet(R request);

        /**
         * Applies the inbound filters to the given InboundInvocation.
         *
         * @param invocation the InboundInvocation to which the filters will be applied
         */
        protected abstract void process(I invocation);
    }

    /**
     * AbstractOutboundInterceptor serves as the base class for all outbound interceptors within the framework.
     * Outbound interceptors are responsible for processing outgoing requests after they have been dispatched
     * and before they are sent to the destination.
     *
     * @param <R> The type of OutboundRequest to be processed by this interceptor.
     * @param <O> The type of OutboundInvocation that will be created and processed.
     */
    public static abstract class AbstractOutboundInterceptor<R extends OutboundRequest, O extends OutboundInvocation<R>> extends AbstractInterceptor {

        /**
         * An array of outbound filters that will be applied to each outbound request.
         */
        protected final OutboundFilter[] outboundFilters;

        /**
         * Constructs a new AbstractOutboundInterceptor with the given InvocationContext and outbound filters.
         *
         * @param context the InvocationContext for the current invocation
         * @param filters a list of OutboundFilter instances to be applied to the outbound request
         */
        public AbstractOutboundInterceptor(InvocationContext context, List<OutboundFilter> filters) {
            super(context);
            this.outboundFilters = filters == null ? new OutboundFilter[0] : filters.toArray(new OutboundFilter[0]);
        }

        /**
         * Processes the outbound request by first creating an OutboundInvocation and then applying the outbound filters.
         *
         * @param request the outbound request to be processed
         * @return the resulting OutboundInvocation after processing
         */
        protected O process(R request) {
            O invocation = createOutlet(request);
            process(invocation);
            return invocation;
        }

        /**
         * Creates a new OutboundInvocation for the given outbound request.
         * This method must be implemented by concrete subclasses to provide the specific type of OutboundInvocation.
         *
         * @param request the outbound request for which to create an OutboundInvocation
         * @return a new OutboundInvocation instance
         */
        protected abstract O createOutlet(R request);

        /**
         * Applies the outbound filters to the given OutboundInvocation.
         * This method must be implemented by concrete subclasses to define how the outbound filters are applied.
         *
         * @param invocation the OutboundInvocation to which the filters will be applied
         */
        protected abstract void process(O invocation);

        protected abstract Response createResponse(Object result, Throwable throwable);

        /**
         * Create a retry supplier
         *
         * @param ctx The {@link MethodContext} containing information about the target
         *            method to be invoked, its arguments, and the expected result type.
         * @return Returns a supplier for retry logic.
         */
        protected Supplier<Response> createRetrySupplier(MethodContext ctx) {
            return () -> {
                Response response = null;
                try {
                    response = createResponse(ctx.invoke(), null);
                } catch (IllegalAccessException ignored) {
                    // ignored
                } catch (Throwable throwable) {
                    response = createResponse(ctx.getResult(), throwable);
                } finally {
                    if (response != null) {
                        response.copyAttribute(ctx);
                    }
                }
                return response;
            };
        }

        /**
         * Invokes a method with retry logic based on the provided {@code invocation} and context.
         * If the initial attempt fails, the method will retry the operation according to the
         * associated {@link RetryPolicy} retrieved from the service metadata.
         *
         * @param invocation The invocation context which may contain service metadata including
         *                   the retry policy. It could be null, in which case no retry policy
         *                   is applied.
         * @param ctx        The {@link MethodContext} containing information about the target
         *                   method to be invoked, its arguments, and the expected result type.
         * @return The response object from the successful invocation or the last retry
         * attempt. Returns null if the response is null or if no retry is
         * performed and the initial attempt fails.
         */
        protected Object invokeWithRetry(O invocation, MethodContext ctx) {
            Supplier<Response> retrySupplier = createRetrySupplier(ctx);
            ServicePolicy servicePolicy = invocation == null ? null : invocation.getServiceMetadata().getServicePolicy();
            RetryPolicy retryPolicy = servicePolicy == null ? null : servicePolicy.getRetryPolicy();
            if (retryPolicy != null && retryPolicy.isEnabled()) {
                RetrierFactory retrierFactory = context.getOrDefaultRetrierFactory(retryPolicy.getType());
                Retrier retrier = retrierFactory == null ? null : retrierFactory.get(retryPolicy);
                if (retrier != null) {
                    Long timeout = retryPolicy.getTimeout();
                    if (timeout != null && timeout > 0) {
                        RequestContext.getOrCreate().setAttribute(Carrier.ATTRIBUTE_DEADLINE, System.currentTimeMillis() + timeout);
                    }
                    return retrier.execute(retrySupplier);
                }
            }
            Response response = retrySupplier.get();
            return response == null ? null : response.getResponse();
        }

    }

    /**
     * AbstractRouteInterceptor is an abstract base class for route interceptors within the framework.
     * Route interceptors are used to determine the route for an outbound request and to apply route-specific filters.
     *
     * @param <R> The type of OutboundRequest to be processed by this interceptor.
     * @param <O> The type of OutboundInvocation that will be created and processed.
     */
    public static abstract class AbstractRouteInterceptor<R extends OutboundRequest, O extends OutboundInvocation<R>> extends AbstractInterceptor {

        /**
         * An array of route filters that will be applied during the routing process.
         */
        protected final RouteFilter[] routeFilters;

        /**
         * Constructs a new AbstractRouteInterceptor with the given InvocationContext and route filters.
         *
         * @param context the InvocationContext for the current invocation
         * @param filters a list of RouteFilter instances to be applied during routing
         */
        public AbstractRouteInterceptor(InvocationContext context, List<RouteFilter> filters) {
            super(context);
            this.routeFilters = filters == null ? new RouteFilter[0] : filters.toArray(new RouteFilter[0]);
        }

        /**
         * Initiates the routing process for the outbound request by creating an OutboundInvocation.
         *
         * @param request the outbound request to be routed
         * @return the resulting OutboundInvocation after initiating the routing process
         */
        protected O routing(R request) {
            O invocation = createOutlet(request);
            routing(invocation);
            return invocation;
        }

        /**
         * Initiates the routing process for the outbound request by creating an OutboundInvocation and providing a list of candidate endpoints.
         *
         * @param request   the outbound request to be routed
         * @param instances a list of Endpoint instances to be considered during routing
         * @return the resulting OutboundInvocation after initiating the routing process
         */
        protected O routing(R request, List<? extends Endpoint> instances) {
            O invocation = createOutlet(request, instances);
            routing(invocation);
            return invocation;
        }

        /**
         * Applies the route filters to the given OutboundInvocation.
         * This method must be implemented by concrete subclasses to define how the route filters are applied.
         *
         * @param invocation the OutboundInvocation to which the filters will be applied
         */
        protected abstract void routing(O invocation);

        /**
         * Creates a new OutboundInvocation for the given outbound request.
         * This method must be implemented by concrete subclasses to provide the specific type of OutboundInvocation.
         *
         * @param request the outbound request for which to create an OutboundInvocation
         * @return a new OutboundInvocation instance
         */
        protected abstract O createOutlet(R request);

        /**
         * Creates a new OutboundInvocation for the given outbound request and initializes it with a list of Endpoint instances.
         * This method can be overridden by concrete subclasses if additional initialization is required.
         *
         * @param request   the outbound request for which to create an OutboundInvocation
         * @param instances a list of Endpoint instances to be associated with the OutboundInvocation
         * @return a new OutboundInvocation instance with the specified Endpoint instances
         */
        protected O createOutlet(R request, List<? extends Endpoint> instances) {
            O result = createOutlet(request);
            result.setInstances(instances);
            return result;
        }
    }

    /**
     * AbstractHttpInboundInterceptor is an abstract base class for inbound interceptors that specifically handle HTTP inbound requests.
     * It extends the functionality of AbstractInboundInterceptor by providing HTTP-specific processing.
     *
     * @param <T> The type of HttpInboundRequest to be processed by this interceptor.
     */
    public static abstract class AbstractHttpInboundInterceptor<T extends HttpInboundRequest> extends AbstractInboundInterceptor<T, HttpInboundInvocation<T>> {

        /**
         * Constructs a new AbstractHttpInboundInterceptor with the given InvocationContext and a list of inbound filters.
         *
         * @param context the InvocationContext for the current invocation
         * @param filters a list of InboundFilter instances to be applied to the inbound request
         */
        public AbstractHttpInboundInterceptor(InvocationContext context, List<InboundFilter> filters) {
            super(context, filters);
        }

        @Override
        protected HttpInboundInvocation<T> createInlet(T request) {
            return new HttpInboundInvocation<>(request, context);
        }

        @Override
        protected void process(HttpInboundInvocation<T> invocation) {
            new InboundFilterChain.Chain(inboundFilters).filter(invocation);
        }
    }

    /**
     * The AbstractHttpOutboundInterceptor is an abstract class that serves as the foundation for implementing interceptors
     * that handle HTTP outbound requests. It extends the functionality of AbstractOutboundInterceptor by providing
     * specific handling for HTTP requests.
     *
     * @param <T> The type parameter for the HttpOutboundRequest that this interceptor will process.
     */
    public static abstract class AbstractHttpOutboundInterceptor<T extends HttpOutboundRequest> extends AbstractOutboundInterceptor<T, HttpOutboundInvocation<T>> {

        /**
         * Constructs a new instance of AbstractHttpOutboundInterceptor with the specified InvocationContext and a list
         * of outbound filters.
         *
         * @param context The InvocationContext associated with the current invocation.
         * @param filters The list of OutboundFilter instances that will be applied to the outbound request.
         */
        public AbstractHttpOutboundInterceptor(InvocationContext context, List<OutboundFilter> filters) {
            super(context, filters);
        }

        @Override
        protected HttpOutboundInvocation<T> createOutlet(T request) {
            return new HttpOutboundInvocation<>(request, context);
        }

        @Override
        protected void process(HttpOutboundInvocation<T> invocation) {
            new OutboundFilterChain.Chain(outboundFilters).filter(invocation);
        }
    }

    /**
     * AbstractHttpRouteInterceptor is an abstract class that provides a base implementation for route interceptors
     * that are specifically designed to handle HTTP outbound requests. It extends the functionality of the
     * AbstractRouteInterceptor class to address the nuances of HTTP routing.
     *
     * @param <T> The type parameter for the HttpOutboundRequest that this interceptor will process.
     */
    public static abstract class AbstractHttpRouteInterceptor<T extends HttpOutboundRequest> extends AbstractRouteInterceptor<T, HttpOutboundInvocation<T>> {

        /**
         * Constructs a new AbstractHttpRouteInterceptor with the given InvocationContext and a list of route filters.
         *
         * @param context The InvocationContext for the current invocation.
         * @param filters The list of RouteFilter instances to be used for routing decisions.
         */
        public AbstractHttpRouteInterceptor(InvocationContext context, List<RouteFilter> filters) {
            super(context, filters);
        }

        @Override
        protected HttpOutboundInvocation<T> createOutlet(T request) {
            return new HttpOutboundInvocation<>(request, context);
        }

        /**
         * Creates a new HttpOutboundInvocation with the specified HttpOutboundRequest and a list of endpoint instances.
         * This method allows setting the endpoint instances for the invocation, which can be used during the routing process.
         *
         * @param request   The HttpOutboundRequest for which to create an invocation.
         * @param instances A list of Endpoint instances to be associated with the invocation.
         * @return A new HttpOutboundInvocation instance with the specified endpoints.
         */
        protected HttpOutboundInvocation<T> createOutlet(T request, List<? extends Endpoint> instances) {
            HttpOutboundInvocation<T> result = createOutlet(request);
            result.setInstances(instances);
            return result;
        }

        @Override
        protected void routing(HttpOutboundInvocation<T> invocation) {
            new RouteFilterChain.Chain(routeFilters).filter(invocation);
        }
    }

    /**
     * AbstractGatewayInterceptor is an abstract class that serves as a foundation for creating interceptors
     * within a gateway environment. It handles both inbound and outbound request processing, providing
     * a common structure for intercepting and modifying requests as they enter and exit the system.
     *
     * @param <I> The type parameter for the HttpInboundRequest that this interceptor will receive.
     * @param <O> The type parameter for the OutboundRequest that this interceptor will create.
     */
    public static abstract class AbstractGatewayInterceptor<I extends HttpInboundRequest, O extends OutboundRequest> extends AbstractInterceptor {

        /**
         * The inbound filters associated with this interceptor.
         */
        protected final InboundFilter[] inboundFilters;

        /**
         * The route filters associated with this interceptor.
         */
        protected final RouteFilter[] routeFilters;

        /**
         * Constructs a new AbstractGatewayInterceptor with the given InvocationContext and lists of inbound and route filters.
         *
         * @param context        The InvocationContext for the current invocation.
         * @param inboundFilters The list of InboundFilter instances to be applied to inbound requests.
         * @param routeFilters   The list of RouteFilter instances to be applied to the routing process.
         */
        public AbstractGatewayInterceptor(InvocationContext context, List<InboundFilter> inboundFilters, List<RouteFilter> routeFilters) {
            super(context);
            this.inboundFilters = inboundFilters == null ? new InboundFilter[0] : inboundFilters.toArray(new InboundFilter[0]);
            this.routeFilters = routeFilters == null ? new RouteFilter[0] : routeFilters.toArray(new RouteFilter[0]);
        }

        /**
         * Creates a new HttpInboundInvocation for the specified HttpInboundRequest.
         * This method should be implemented by extending classes to provide the necessary invocation context for inbound requests.
         *
         * @param request The HttpInboundRequest for which to create an invocation.
         * @return A new HttpInboundInvocation instance for the request.
         */
        protected HttpInboundInvocation<I> createInlet(I request) {
            return new GatewayInboundInvocation<>(request, context);
        }

        /**
         * Creates an OutboundInvocation for the specified HttpInboundRequest. This method should be implemented by extending classes
         * to define how an outbound request is created from an inbound request.
         *
         * @param request The HttpInboundRequest used to create an OutboundInvocation.
         * @return An OutboundInvocation instance, or null if the implementation is not provided.
         */
        protected OutboundInvocation<O> createOutlet(I request) {
            return null; // This must be overridden by extending classes.
        }

        /**
         * Processes the inbound request by creating a HttpInboundInvocation and applying the inbound filters to it.
         *
         * @param request The inbound request to be processed.
         * @return The processed HttpInboundInvocation instance.
         */
        protected HttpInboundInvocation<I> process(I request) {
            HttpInboundInvocation<I> invocation = createInlet(request);
            process(invocation);
            return invocation;
        }

        /**
         * Applies the inbound filters to the given HttpInboundInvocation.
         *
         * @param invocation The HttpInboundInvocation to which the filters will be applied.
         */
        protected void process(HttpInboundInvocation<I> invocation) {
            new InboundFilterChain.Chain(inboundFilters).filter(invocation);
        }

        /**
         * Routes the outbound request by creating an OutboundInvocation and applying the route filters to it.
         *
         * @param request The inbound request from which to create an OutboundInvocation.
         * @return The routed OutboundInvocation instance.
         */
        protected OutboundInvocation<? extends O> routing(I request) {
            OutboundInvocation<O> invocation = createOutlet(request);
            routing(invocation);
            return invocation;
        }

        /**
         * Applies the route filters to the given OutboundInvocation as part of the routing process.
         *
         * @param invocation The OutboundInvocation to which the route filters will be applied.
         */
        protected void routing(OutboundInvocation<? extends O> invocation) {
            new RouteFilterChain.Chain(routeFilters).filter(invocation);
        }
    }

}
