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

import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    boolean isLiveEnabled();

    boolean isLaneEnabled();

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
     *      * or returns the default loadbalancer instance if no loadbalancer is found with that name.
     *
     * @param name The name of the loadbalancer.
     * @return the {@code LoadBalancer} instance associated with the given name, or the
     *         default loadbalancer if no matching name is found.
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
     * Outbound filters are applied to responses before they are sent back to the client. These filters can be used
     * for tasks such as adding headers, transforming response bodies, or logging. Similar to inbound filters, the
     * order in the array might determine the sequence in which these filters are executed.
     * </p>
     *
     * @return An array of {@link OutboundFilter} instances that are configured for the service. The list may be empty
     * if no outbound filters are configured.
     */
    OutboundFilter[] getOutboundFilters();

    /**
     * Retrieves an array of route filters.
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
     * Processes an outbound invocation through a chain of configured outbound filters.
     * <p>
     * This method initiates the filtering process for outbound requests, which could include modifications,
     * logging, validation, or any custom processing defined in the outbound filters. The filters are applied
     * in the order they are defined within the {@link OutboundFilterChain}.
     * </p>
     *
     * @param <R>        the type of the outbound request extending {@link OutboundRequest}.
     * @param invocation the outbound invocation context containing the request to be processed and
     *                   other relevant information. It is the input to the first filter in the chain.
     */
    default <R extends OutboundRequest> void outbound(OutboundInvocation<R> invocation) {
        OutboundFilterChain.Chain chain = new OutboundFilterChain.Chain(getOutboundFilters());
        chain.filter(invocation);
    }

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
    default <R extends InboundRequest> void inbound(InboundInvocation<R> invocation) {
        InboundFilterChain.Chain chain = new InboundFilterChain.Chain(getInboundFilters());
        chain.filter(invocation);
    }

    /**
     * Applies route filters to the specified {@link OutboundInvocation} and identifies endpoints that are suitable targets for the request.
     *
     * @param <R>        the type parameter extending {@link OutboundRequest}, representing the specific type of request being routed. This
     *                   ensures that the method can handle various request types, providing flexibility in the routing logic.
     * @param invocation the {@code OutboundInvocation<R>} to which the route filters are to be applied. This encapsulates both the request
     *                   to be routed and its initially considered endpoints, serving as the input for the filtering process.
     * @param instances  an initial list of {@code Endpoint}s considered for the invocation. This list is subject to filtering by the route
     *                   filters to determine the final list of suitable endpoints. It can be null or empty, in which case the method may
     *                   apply default logic or return an empty list.
     * @return A list of {@link Endpoint}s deemed suitable for the invocation after the application of route filters. This list represents the
     * filtered set of endpoints that have passed through the route filtering process and are considered appropriate targets for the
     * request. The list may be empty if no endpoints are found to be suitable based on the applied filters.
     */
    default <R extends OutboundRequest> List<? extends Endpoint> route(OutboundInvocation<R> invocation, List<? extends Endpoint> instances) {
        return route(invocation, instances, null);
    }

    /**
     * Applies route filters to the given {@link OutboundInvocation} and retrieves the endpoints that are determined to be suitable targets.
     *
     * @param <R>        the type parameter extending {@link OutboundRequest}, representing the specific type of request being routed
     * @param invocation the {@code OutboundInvocation} to which the route filters are to be applied, encompassing the request and
     *                   its initially considered endpoints
     * @return A list of {@link Endpoint}s deemed suitable for the invocation after the application of route filters. This list may be
     * empty if the filters conclude that no endpoints are suitable.
     */
    default <R extends OutboundRequest> List<? extends Endpoint> route(OutboundInvocation<R> invocation) {
        return route(invocation, null, null);
    }

    /**
     * Routes an outbound request through a series of {@link RouteFilter}s to determine the appropriate
     * {@link Endpoint}s for the request. This method applies the provided filters (or the default
     * filters if none are provided) to the given list of instances (endpoints), modifying the
     * invocation's endpoints based on the filtering logic.
     *
     * @param <R>        The type of the outbound request, extending {@link OutboundRequest}.
     * @param invocation The {@link OutboundInvocation} representing the outbound request and
     *                   containing information necessary for routing.
     * @param instances  A list of initial {@link Endpoint} instances to be considered for the request.
     *                   If {@code null} or empty, the default list of endpoints will be used.
     * @param filters    A collection of {@link RouteFilter} instances to apply to the endpoints. If
     *                   {@code null} or empty, the default set of route filters is used.
     * @return An array of {@link Endpoint} instances that have been filtered according to the
     * specified (or default) filters and are deemed suitable for the outbound request.
     * @throws IllegalArgumentException if the invocation or any other required parameter is {@code null}.
     */
    default <R extends OutboundRequest> List<? extends Endpoint> route(OutboundInvocation<R> invocation,
                                                                       List<? extends Endpoint> instances,
                                                                       RouteFilter[] filters) {
        if (instances != null && !instances.isEmpty()) {
            invocation.setInstances(instances);
        }
        RouteFilterChain.Chain chain = new RouteFilterChain.Chain(filters == null || filters.length == 0 ? getRouteFilters() : filters);
        chain.filter(invocation);
        return invocation.getEndpoints();
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
        public GovernanceConfig getGovernanceConfig() {
            return delegate.getGovernanceConfig();
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
        public OutboundFilter[] getOutboundFilters() {
            return delegate.getOutboundFilters();
        }

        @Override
        public RouteFilter[] getRouteFilters() {
            return delegate.getRouteFilters();
        }

        @Override
        public <R extends OutboundRequest> ClusterInvoker getClusterInvoker(OutboundInvocation<R> invocation,
                                                                            ClusterPolicy defaultPolicy) {
            return delegate.getClusterInvoker(invocation, defaultPolicy);
        }

        @Override
        public <R extends OutboundRequest> void outbound(OutboundInvocation<R> invocation) {
            delegate.outbound(invocation);
        }

        @Override
        public <R extends InboundRequest> void inbound(InboundInvocation<R> invocation) {
            delegate.inbound(invocation);
        }

        @Override
        public <R extends OutboundRequest> List<? extends Endpoint> route(OutboundInvocation<R> invocation,
                                                                          List<? extends Endpoint> instances) {
            return delegate.route(invocation, instances, null);
        }

        @Override
        public <R extends OutboundRequest> List<? extends Endpoint> route(OutboundInvocation<R> invocation) {
            return delegate.route(invocation, null, null);
        }

        @Override
        public <R extends OutboundRequest> List<? extends Endpoint> route(OutboundInvocation<R> invocation,
                                                                          List<? extends Endpoint> instances,
                                                                          RouteFilter[] filters) {
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
        public <R extends OutboundRequest> List<? extends Endpoint> route(OutboundInvocation<R> invocation,
                                                                          List<? extends Endpoint> instances,
                                                                          RouteFilter[] filters) {
            List<? extends Endpoint> result = super.route(invocation, instances, filters);
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
