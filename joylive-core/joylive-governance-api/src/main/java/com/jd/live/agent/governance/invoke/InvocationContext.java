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
package com.jd.live.agent.governance.invoke;

import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.counter.CounterManager;
import com.jd.live.agent.governance.invoke.filter.*;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.invoke.matcher.TagMatcher;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.domain.Domain;
import com.jd.live.agent.governance.policy.domain.DomainPolicy;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.policy.variable.VariableFunction;
import com.jd.live.agent.governance.policy.variable.VariableParser;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * The {@code InvocationContext} interface defines a contract for an invocation context in a component-based application.
 * It provides methods to retrieve various configurations and functional interfaces that are relevant to the invocation
 * of components, such as application information, governance configurations, policy suppliers, and various functional
 * mappings for unit functions, variable functions, variable parsers, and tag matchers.
 */
public interface InvocationContext {

    /**
     * The constant string representing the key used to store or retrieve the InvocationContext.
     */
    String COMPONENT_INVOCATION_CONTEXT = "invocationContext";

    /**
     * Retrieves the application information associated with this invocation context.
     *
     * @return An instance of {@code Application} representing the application information.
     */
    Application getApplication();

    default Location getLocation() {
        return getApplication().getLocation();
    }

    /**
     * Checks if the live feature is enabled.
     *
     * @return {@code true} if the live feature is enabled, {@code false} otherwise
     */
    boolean isLiveEnabled();

    /**
     * Checks if the lane feature is enabled.
     *
     * @return {@code true} if the lane feature is enabled, {@code false} otherwise
     */
    boolean isLaneEnabled();

    /**
     * Checks if the flow control feature is enabled.
     *
     * @return {@code true} if the flow control feature is enabled, {@code false} otherwise
     */
    boolean isFlowControlEnabled();

    /**
     * Checks if the governance context is ready.
     * <p>
     * This default implementation checks if the application's status is {@code AppStatus.READY}.
     * </p>
     *
     * @return {@code true} if the governance context is ready, {@code false} otherwise
     */
    default boolean isGovernReady() {
        return getApplication().getStatus() == AppStatus.READY;
    }

    /**
     * Retrieves the governance configuration associated with this invocation context.
     *
     * @return An instance of {@code GovernanceConfig} representing the governance configurations.
     */
    GovernanceConfig getGovernanceConfig();

    /**
     * Returns a timer instance used for measuring and recording the duration of events within the
     * system. This timer can be used to track metrics such as request latency, service response times,
     * or other performance-related measurements.
     *
     * @return A timer instance that can be used to measure and record event durations.
     */
    Timer getTimer();

    CounterManager getCounterManager();

    /**
     * Retrieves a {@link Publisher} that emits {@link TrafficEvent} instances.
     *
     * @return A {@link Publisher} that provides a stream of {@link TrafficEvent} objects.
     */
    Publisher<TrafficEvent> getTrafficPublisher();

    /**
     * Retrieves the policy supplier associated with this invocation context.
     *
     * @return An instance of {@code PolicySupplier} that supplies policies.
     */
    PolicySupplier getPolicySupplier();

    /**
     * Retrieves a unit function by its name.
     *
     * @param name The name of the unit function.
     * @return The requested unit function, or null if not found.
     */
    UnitFunction getUnitFunction(String name);

    /**
     * Retrieves a variable function by its name.
     *
     * @param name The name of the variable function.
     * @return The requested variable function, or null if not found.
     */
    VariableFunction getVariableFunction(String name);

    /**
     * Retrieves a variable parser by its name.
     *
     * @param name The name of the variable parser.
     * @return The requested variable parser, or null if not found.
     */
    VariableParser<?, ?> getVariableParser(String name);

    /**
     * Retrieves a map of tag matchers associated with this invocation context.
     *
     * @return A map of strings to {@code TagMatcher} instances.
     */
    Map<String, TagMatcher> getTagMatchers();

    /**
     * Retrieves the {@code LoadBalancer} instance associated with the specified name,
     * * or returns the default loadbalancer instance if no loadbalancer is found with that name.
     *
     * @param name The name of the loadbalancer.
     * @return the {@code LoadBalancer} instance associated with the given name, or the
     * default loadbalancer if no matching name is found.
     */
    LoadBalancer getOrDefaultLoadBalancer(String name);

    /**
     * Retrieves the {@code ClusterInvoker} instance associated with the specified name,
     * * or returns the default ClusterInvoker instance if no ClusterInvoker is found with that name.
     *
     * @param name The name of the loadbalancer.
     * @return the {@code ClusterInvoker} instance associated with the given name, or the
     * default ClusterInvoker if no matching name is found.
     */
    ClusterInvoker getOrDefaultClusterInvoker(String name);

    /**
     * Retrieves a {@link ClusterInvoker} based on the cluster policy associated with the provided service invocation.
     * <p>
     * This method determines the appropriate {@link ClusterInvoker} to use for a given service invocation by examining
     * the service's metadata to extract a cluster policy. The type of cluster policy (if any) guides the selection
     * of a suitable {@link ClusterInvoker} from the context. If the invocation or its associated policies are not
     * specified (i.e., {@code null}), or if no specific {@link ClusterInvoker} is defined for the policy type, a
     * default {@link ClusterInvoker} is returned from the context.
     * </p>
     *
     * @param invocation    The service invocation object, which may contain metadata about the service and its policies.
     *                      This parameter can be {@code null}.
     * @param defaultPolicy The default cluster policy.
     * @param <R>           The type of the outbound request.
     * @return A {@link ClusterInvoker} that is best suited to handle the service request based on the defined
     * cluster policy. If no specific policy is found, a default invoker is returned.
     */
    default <R extends OutboundRequest> ClusterInvoker getClusterInvoker(OutboundInvocation<R> invocation, ClusterPolicy defaultPolicy) {
        ServicePolicy servicePolicy = invocation == null ? null : invocation.getServiceMetadata().getServicePolicy();
        ClusterPolicy clusterPolicy = servicePolicy == null ? null : servicePolicy.getClusterPolicy();
        clusterPolicy = servicePolicy == null ? defaultPolicy : clusterPolicy;
        String name = clusterPolicy == null ? null : clusterPolicy.getType();
        return getOrDefaultClusterInvoker(name);
    }

    /**
     * Retrieves an array of inbound filters.
     * <p>
     * Inbound filters are applied to requests as they are received by the service. These filters can perform
     * various tasks such as logging, authentication, and request validation. The order in the array may determine
     * the order in which these filters are applied.
     * </p>
     *
     * @return An array of {@link InboundFilter} instances that are configured for the service. The list may be empty
     * if no inbound filters are configured.
     */
    InboundFilter[] getInboundFilters();

    /**
     * Retrieves an array of outbound filters.
     * <p>
     * Route filters are used to determine how requests are routed within the service or to external services.
     * They can be used for tasks such as load balancing, service discovery, and request redirection. The order
     * in the array can influence the priority of routing decisions.
     * </p>
     *
     * @return An array of {@link RouteFilter} instances that are used for routing decisions. The list may be empty
     * if no route filters are configured.
     */
    RouteFilter[] getRouteFilters();

    /**
     * Retrieves an array of outbound filters.
     *
     * @return An array of {@link OutboundFilter} instances that are used for outbound request modification. The list may be empty
     * if no outbound filters are configured.
     */
    OutboundFilter[] getOutboundFilters();

    /**
     * Processes an inbound invocation through a chain of configured inbound filters.
     * <p>
     * Similar to the {@code outbound} method, this method facilitates the processing of inbound requests
     * through a series of filters. These filters can perform various tasks such as authentication, logging,
     * validation, and more, according to the needs of the application. The inbound filters are executed in
     * the sequence they are arranged in the {@link InboundFilterChain}.
     * </p>
     *
     * @param <R>        the type of the inbound request extending {@link InboundRequest}.
     * @param invocation the inbound invocation context containing the request to be processed along
     *                   with any associated data. This context is passed through the filter chain for
     *                   processing.
     */
    default <R extends InboundRequest> CompletionStage<Object> inbound(InboundInvocation<R> invocation) {
        return inbound(invocation, null);
    }

    /**
     * Processes an inbound invocation through a chain of configured inbound filters and invokes a callable object.
     * <p>
     * This method is similar to the other {@code inbound} method, but it also allows you to specify a callable object
     * that will be invoked after all the filters have been processed. This can be useful if you need to perform some
     * additional processing or logic after the filters have run.
     * </p>
     *
     * @param <R>        the type of the inbound request extending {@link InboundRequest}.
     * @param invocation the inbound invocation context containing the request to be processed along
     *                   with any associated data. This context is passed through the filter chain for
     *                   processing.
     * @param callable   the callable object to invoke after all the filters have been processed.
     * @return a completion stage that represents the result of the inbound invocation.
     */
    default <R extends InboundRequest> CompletionStage<Object> inbound(InboundInvocation<R> invocation, Callable<Object> callable) {
        try {
            InboundFilter[] filters = getInboundFilters();
            InboundFilterChain.Chain chain = callable == null
                    ? new InboundFilterChain.Chain(filters)
                    : new InboundFilterChain.InvokerChain(filters, callable);
            return chain.filter(invocation).whenComplete((r, t) -> {
                if (t == null) {
                    invocation.onForward();
                } else if (t instanceof RejectException) {
                    invocation.onReject((RejectException) t);
                }
            });
        } catch (RejectException e) {
            invocation.onReject(e);
            return Futures.future(e);
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    /**
     * Processes an inbound invocation through a chain of configured inbound filters and invokes a callable object asynchronously,
     * then applies a function to the result.
     * <p>
     * This method is similar to the other {@code inbound} methods, but it also allows you to specify a function that will be applied
     * to the result of the callable object. This can be useful if you need to transform the result or perform some additional
     * processing based on the result.
     * </p>
     *
     * @param <R>        the type of the inbound request extending {@link InboundRequest}.
     * @param <T>        the type of the result returned by the function.
     * @param invocation the inbound invocation context containing the request to be processed along
     *                   with any associated data. This context is passed through the filter chain for
     *                   processing.
     * @param callable   the callable object to invoke after all the filters have been processed.
     * @param function   the function to apply to the result of the callable object.
     * @return the result of applying the function to the result of the callable object.
     */
    default <R extends InboundRequest, T> T inbound(InboundInvocation<R> invocation,
                                                    Callable<Object> callable,
                                                    Function<CompletionStage<Object>, T> function) {
        try {
            CompletionStage<Object> stage = inbound(invocation, callable);
            return function.apply(stage);
        } catch (Throwable e) {
            return function.apply(Futures.future(e));
        }
    }

    /**
     * Processes an inbound invocation through a chain of configured inbound filters and invokes a callable object synchronously.
     * <p>
     * This method is similar to the {@code inbound} method, but it blocks until the inbound invocation and the callable object
     * have completed. It returns the result of the callable object, or throws any exception that occurred during the processing.
     * </p>
     *
     * @param <R>        the type of the inbound request extending {@link InboundRequest}.
     * @param invocation the inbound invocation context containing the request to be processed along
     *                   with any associated data. This context is passed through the filter chain for
     *                   processing.
     * @param callable   the callable object to invoke after all the filters have been processed.
     * @return the result of the callable object.
     * @throws Throwable if any exception occurs during the processing.
     */
    default <R extends InboundRequest> Object inward(InboundInvocation<R> invocation, Callable<Object> callable) throws Throwable {
        try {
            CompletionStage<Object> stage = inbound(invocation, callable);
            return stage.toCompletableFuture().get();
        } catch (Throwable e) {
            if (e instanceof ExecutionException) {
                if (e.getCause() != null) {
                    Throwable cause = e.getCause();
                    if (cause instanceof InvocationTargetException) {
                        if (cause.getCause() != null) {
                            throw cause.getCause();
                        }
                    }
                    throw cause;
                }
            }
            throw e;
        }
    }

    /**
     * Processes an inbound invocation through a chain of configured inbound filters and invokes a callable object synchronously,
     * then applies a function to the result and the inbound request.
     * <p>
     * This method is similar to the other {@code inward} method, but it also allows you to specify a function that will be applied
     * to the result of the callable object and the inbound request. This can be useful if you need to transform the result or
     * perform some additional processing based on the inbound request.
     * </p>
     *
     * @param <R>        the type of the inbound request extending {@link InboundRequest}.
     * @param <T>        the type of the result returned by the function.
     * @param invocation the inbound invocation context containing the request to be processed along
     *                   with any associated data. This context is passed through the filter chain for
     *                   processing.
     * @param callable   the callable object to invoke after all the filters have been processed.
     * @param function   the function to apply to the result of the callable object.
     * @return the result of applying the function to the result of the callable object and the inbound request.
     */
    default <R extends InboundRequest, T> T inward(InboundInvocation<R> invocation,
                                                   Callable<Object> callable,
                                                   Function<Object, T> function) {
        try {
            return function.apply(inward(invocation, callable));
        } catch (Throwable e) {
            return function.apply(e);
        }
    }

    /**
     * Applies route filters to the specified {@link OutboundInvocation} and identifies endpoints that are suitable targets for the request.
     *
     * @param <R>        the type parameter extending {@link OutboundRequest}, representing the specific type of request being routed. This
     *                   ensures that the method can handle various request types, providing flexibility in the routing logic.
     * @param <E>        the type parameter extending {@link Endpoint}, representing the specific type of endpoint.
     * @param invocation the {@code OutboundInvocation<R>} to which the route filters are to be applied.
     * @param instances  an initial list of {@code Endpoint}s considered for the invocation.
     * @return An {@link Endpoint} instance deemed suitable for the invocation after the application of route filters, or {@code null} if no suitable endpoint is found.
     * @throws RejectNoProviderException if no provider is found for the invocation.
     * @throws RejectException           if the request is rejected during filtering.
     */
    default <R extends OutboundRequest, E extends Endpoint> E route(OutboundInvocation<R> invocation, List<E> instances) {
        return route(invocation, instances, (RouteFilter[]) null);
    }

    /**
     * Applies route filters to the specified {@link OutboundInvocation} and identifies endpoints that are suitable targets for the request.
     * This method allows for the conversion of instances of type {@code P} to {@code E} using the provided converter function before applying filters.
     *
     * @param <R>        the type parameter extending {@link OutboundRequest}, representing the specific type of request being routed. This
     *                   ensures that the method can handle various request types, providing flexibility in the routing logic.
     * @param <E>        the type parameter extending {@link Endpoint}, representing the specific type of endpoint.
     * @param <P>        the type parameter representing the type of the initial instances to be converted.
     * @param invocation the {@code OutboundInvocation<R>} to which the route filters are to be applied.
     * @param instances  a list of initial instances of type {@code P} considered for the invocation.
     * @param converter  a {@code Function<P, E>} used to convert instances of type {@code P} to {@code E}.
     * @return An {@link Endpoint} instance deemed suitable for the invocation after the application of route filters, or {@code null} if no suitable endpoint is found.
     * @throws RejectNoProviderException if no provider is found for the invocation.
     * @throws RejectException           if the request is rejected during filtering.
     */
    default <R extends OutboundRequest,
            E extends Endpoint, P> E route(OutboundInvocation<R> invocation, List<P> instances, Function<P, E> converter) {
        List<E> endpoints = instances == null ? new ArrayList<>() : new ArrayList<>(instances.size());
        if (instances != null) {
            for (P instance : instances) {
                endpoints.add(converter.apply(instance));
            }
        }
        return route(invocation, endpoints, (RouteFilter[]) null);
    }

    /**
     * Applies route filters to the given {@link OutboundInvocation} and retrieves the endpoints that are determined to be suitable targets.
     *
     * @param <R>        the type parameter extending {@link OutboundRequest}, representing the specific type of request being routed.
     * @param <E>        the type parameter extending {@link Endpoint}, representing the specific type of endpoint.
     * @param invocation the {@code OutboundInvocation} to which the route filters are to be applied, encompassing the request and
     *                   its initially considered endpoints.
     * @return An {@link Endpoint} instance deemed suitable for the invocation after the application of route filters, or {@code null} if no suitable endpoint is found.
     * @throws RejectNoProviderException if no provider is found for the invocation.
     * @throws RejectException           if the request is rejected during filtering.
     */
    default <R extends OutboundRequest, E extends Endpoint> E route(OutboundInvocation<R> invocation) {
        return route(invocation, null, (RouteFilter[]) null);
    }

    /**
     * Routes an outbound request through a series of {@link RouteFilter}s to determine the appropriate
     * {@link Endpoint}s for the request. This method applies the provided filters (or the default
     * filters if none are provided) to the given list of instances (endpoints), modifying the
     * invocation's endpoints based on the filtering logic.
     *
     * @param <R>            The type of the outbound request, extending {@link OutboundRequest}.
     * @param <E>            The type of the endpoints, extending {@link Endpoint}.
     * @param invocation     The {@link OutboundInvocation} representing the outbound request and
     *                       containing information necessary for routing.
     * @param instances      A list of initial {@link Endpoint} instances to be considered for the request.
     * @param filters        A collection of {@link RouteFilter} instances to apply to the endpoints. If
     *                       {@code null} or empty, the default set of route filters is used.
     * @return An {@link Endpoint} instance that has been filtered according to the
     * specified (or default) filters and is deemed suitable for the outbound request.
     * @throws RejectNoProviderException if no provider is found for the invocation.
     * @throws RejectException           if the request is rejected during filtering.
     */
    @SuppressWarnings("unchecked")
    default <R extends OutboundRequest,
            E extends Endpoint> E route(OutboundInvocation<R> invocation, List<E> instances, RouteFilter[] filters) {
        if (instances != null && !instances.isEmpty()) {
            invocation.setInstances(instances);
        }
        try {
            RouteFilterChain chain = new RouteFilterChain.Chain(filters == null || filters.length == 0 ? getRouteFilters() : filters);
            chain.filter(invocation);
            List<? extends Endpoint> endpoints = invocation.getEndpoints();
            Endpoint endpoint = endpoints != null && !endpoints.isEmpty() ? endpoints.get(0) : null;
            if (endpoint != null || !invocation.getRequest().isInstanceSensitive()) {
                invocation.onForward(endpoint);
                return (E) endpoint;
            } else {
                throw new RejectNoProviderException("There is no provider for invocation " + invocation.getRequest().getService());
            }
        } catch (RejectException e) {
            invocation.onReject(e);
            throw e;
        }
    }

    /**
     * Processes an outbound invocation through a chain of configured outbound filters.
     * <p>
     * This method is similar to the {@code outbound} method, but it allows you to pass an existing
     * {@link OutboundInvocation} object instead of creating a new one. It facilitates the processing of
     * outbound requests through a series of filters. These filters can perform various tasks such as
     * authentication, logging, validation, and more, according to the needs of the application. The outbound
     * filters are executed in the sequence they are arranged in the {@link OutboundFilterChain}.
     * </p>
     *
     * @param <R>        The type of the outbound request, which must extend {@link OutboundRequest}.
     * @param <O>        The type of the outbound response.
     * @param <E>        The type of the endpoint to which requests are routed.
     * @param invocation The outbound service request invocation to be processed.
     * @param endpoint   The endpoint through which the request will be sent.
     * @param callable   The callable object to invoke after all the filters have been processed.
     * @return A CompletionStage that will contain the outbound service response when the request is completed.
     */
    default <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> outbound(OutboundInvocation<R> invocation, E endpoint, Callable<Object> callable) {
        try {
            OutboundFilterChain chain = callable == null
                    ? new OutboundFilterChain.Chain(getOutboundFilters())
                    : new OutboundFilterChain.InvokerChain(getOutboundFilters(), callable);
            return chain.filter(invocation, endpoint);
        } catch (RejectException e) {
            invocation.onReject(e);
            return Futures.future(e);
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    /**
     * Retrieves the current status of the application.
     *
     * @return The current {@link AppStatus} of the application, defaulting to {@code AppStatus.READY} if the status is {@code null}.
     */
    default AppStatus getAppStatus() {
        AppStatus status = getApplication().getStatus();
        return status == null ? AppStatus.READY : status;
    }

    /**
     * A delegate class for {@link InvocationContext} that forwards all its operations to another {@link InvocationContext} instance.
     * This class acts as a wrapper or intermediary, allowing for additional behaviors to be inserted before or after
     * the delegation of method calls. It implements the {@link InvocationContext} interface and can be used
     * anywhere an InvocationContext is required, providing a flexible mechanism for extending or modifying the behavior
     * of invocation contexts dynamically.
     *
     * <p>This delegate pattern is particularly useful for adding cross-cutting concerns like logging, monitoring,
     * or security checks in a transparent manner, without altering the original behavior of the invocation context.</p>
     *
     * @see InvocationContext
     */
    class DelegateContext implements InvocationContext {

        /**
         * The {@link InvocationContext} instance to which this delegate will forward all method calls.
         */
        protected final InvocationContext delegate;

        /**
         * Constructs a new {@code InvocationContextDelegate} with a specified {@link InvocationContext} to delegate to.
         *
         * @param delegate The {@link InvocationContext} instance that this delegate will forward calls to.
         */
        public DelegateContext(InvocationContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public Application getApplication() {
            return delegate.getApplication();
        }

        @Override
        public boolean isLiveEnabled() {
            return delegate.isLiveEnabled();
        }

        @Override
        public boolean isLaneEnabled() {
            return delegate.isLaneEnabled();
        }

        @Override
        public boolean isFlowControlEnabled() {
            return delegate.isFlowControlEnabled();
        }

        @Override
        public GovernanceConfig getGovernanceConfig() {
            return delegate.getGovernanceConfig();
        }

        @Override
        public Timer getTimer() {
            return delegate.getTimer();
        }

        @Override
        public CounterManager getCounterManager() {
            return delegate.getCounterManager();
        }

        @Override
        public Publisher<TrafficEvent> getTrafficPublisher() {
            return delegate.getTrafficPublisher();
        }

        @Override
        public PolicySupplier getPolicySupplier() {
            return delegate.getPolicySupplier();
        }

        @Override
        public UnitFunction getUnitFunction(String name) {
            return delegate.getUnitFunction(name);
        }

        @Override
        public VariableFunction getVariableFunction(String name) {
            return delegate.getVariableFunction(name);
        }

        @Override
        public VariableParser<?, ?> getVariableParser(String name) {
            return delegate.getVariableParser(name);
        }

        @Override
        public Map<String, TagMatcher> getTagMatchers() {
            return delegate.getTagMatchers();
        }

        @Override
        public LoadBalancer getOrDefaultLoadBalancer(String name) {
            return delegate.getOrDefaultLoadBalancer(name);
        }

        @Override
        public ClusterInvoker getOrDefaultClusterInvoker(String name) {
            return delegate.getOrDefaultClusterInvoker(name);
        }

        @Override
        public InboundFilter[] getInboundFilters() {
            return delegate.getInboundFilters();
        }

        @Override
        public RouteFilter[] getRouteFilters() {
            return delegate.getRouteFilters();
        }

        @Override
        public OutboundFilter[] getOutboundFilters() {
            return delegate.getOutboundFilters();
        }

        @Override
        public <R extends OutboundRequest> ClusterInvoker getClusterInvoker(OutboundInvocation<R> invocation,
                                                                            ClusterPolicy defaultPolicy) {
            return delegate.getClusterInvoker(invocation, defaultPolicy);
        }

        @Override
        public <R extends InboundRequest> CompletionStage<Object> inbound(InboundInvocation<R> invocation) {
            return delegate.inbound(invocation);
        }

        @Override
        public <R extends OutboundRequest, E extends Endpoint> E route(OutboundInvocation<R> invocation, List<E> instances) {
            return delegate.route(invocation, instances, (RouteFilter[]) null);
        }

        @Override
        public <R extends OutboundRequest,
                E extends Endpoint, P> E route(OutboundInvocation<R> invocation, List<P> instances, Function<P, E> converter) {
            return InvocationContext.super.route(invocation, instances, converter);
        }

        @Override
        public <R extends OutboundRequest, E extends Endpoint> E route(OutboundInvocation<R> invocation) {
            return delegate.route(invocation, null, (RouteFilter[]) null);
        }

        @Override
        public <R extends OutboundRequest,
                E extends Endpoint> E route(OutboundInvocation<R> invocation, List<E> instances, RouteFilter[] filters) {
            return delegate.route(invocation, instances, filters);
        }

        @Override
        public AppStatus getAppStatus() {
            return delegate.getAppStatus();
        }
    }

    /**
     * An {@code HttpForwardInvocationContext} extends {@code InvocationContextDelegate} to specifically handle
     * the routing of HTTP requests
     *
     * @see DelegateContext
     */
    class HttpForwardContext extends DelegateContext {

        private static final Map<String, Template> TEMPLATES = new ConcurrentHashMap<>();

        /**
         * Constructs an {@code HttpForwardInvocationContext} with the specified delegate context.
         *
         * @param delegate The delegate {@code InvocationContext} to which this context adds additional functionality.
         */
        public HttpForwardContext(InvocationContext delegate) {
            super(delegate);
        }

        @Override
        public <R extends OutboundRequest,
                E extends Endpoint> E route(OutboundInvocation<R> invocation, List<E> instances, RouteFilter[] filters) {
            E result = super.route(invocation, instances, filters);
            if (invocation.getRequest() instanceof HttpOutboundRequest) {
                HttpOutboundRequest request = (HttpOutboundRequest) invocation.getRequest();
                RouteTarget target = invocation.getRouteTarget();
                UnitRoute unitRoute = target.getUnitRoute();
                CellRoute cellRoute = target.getCellRoute();
                Unit unit = unitRoute == null ? null : unitRoute.getUnit();
                Cell cell = cellRoute == null ? null : cellRoute.getCell();
                if (unit != null && cell != null) {
                    String host = request.getHost();
                    String unitHost = getUnitHost(host, invocation.getGovernancePolicy(), unit);
                    if (unitHost == null) {
                        unitHost = request.getForwardHostExpression();
                    }
                    if (unitHost != null) {
                        Template template = TEMPLATES.computeIfAbsent(unitHost, v -> new Template(v, 128));
                        if (template.getVariables() > 0) {
                            Map<String, Object> context = new HashMap<>();
                            context.put("unit", unit.getHostPrefix());
                            context.put("cell", cell.getHostPrefix());
                            context.put("host", host);
                            unitHost = template.evaluate(context);
                        }
                        request.forward(unitHost);
                    }
                }
            }

            return result;
        }

        /**
         * Resolves the host for a given unit based on the incoming request's host, the governance policy,
         * and the unit's information. This method supports dynamic host resolution based on governance policies
         * and is intended for internal use within the routing logic.
         *
         * @param host             The host of the incoming request.
         * @param governancePolicy The governance policy under which the request is being processed.
         * @param unit             The unit associated with the request, if any.
         * @return The resolved host for the unit, or {@code null} if it cannot be resolved.
         */
        protected String getUnitHost(String host, GovernancePolicy governancePolicy, Unit unit) {
            Domain domain = governancePolicy == null ? null : governancePolicy.getDomain(host);
            DomainPolicy domainPolicy = domain == null ? null : domain.getPolicy();
            if (domainPolicy != null) {
                if (domainPolicy.isUnit()) {
                    return domainPolicy.getUnitDomain().getHost();
                } else {
                    UnitDomain unitDomain = domainPolicy.getLiveDomain().getUnitDomain(unit.getCode());
                    return unitDomain == null ? null : unitDomain.getHost();
                }
            }
            return null;
        }
    }

}
