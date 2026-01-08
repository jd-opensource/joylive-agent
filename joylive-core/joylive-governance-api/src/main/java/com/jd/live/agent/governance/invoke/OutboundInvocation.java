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

import com.jd.live.agent.core.instance.GatewayRole;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.event.TrafficEvent.ComponentType;
import com.jd.live.agent.governance.event.TrafficEvent.Direction;
import com.jd.live.agent.governance.event.TrafficEvent.RejectType;
import com.jd.live.agent.governance.event.TrafficEvent.TrafficEventBuilder;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.metadata.parser.LiveMetadataParser.OutboundLiveMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.LiveMetadataParser.RpcOutboundLiveMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.LiveParser;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.ServiceParser;
import com.jd.live.agent.governance.invoke.metadata.parser.ServiceMetadataParser.ForwardServiceMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.ServiceMetadataParser.GatewayOutboundServiceMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.ServiceMetadataParser.OutboundServiceMetadataParser;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.RpcRequest.RpcOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an abstract outbound invocation, extending the Invocation class
 * with outbound-specific properties and methods.
 *
 * @param <T> the type of outbound request associated with this invocation
 */
@Setter
@Getter
public abstract class OutboundInvocation<T extends OutboundRequest> extends Invocation<T> {

    /**
     * A list of endpoints that this outbound invocation targets.
     */
    private List<? extends Endpoint> instances;

    private List<OutboundListener> listeners;

    /**
     * The target route for this outbound invocation.
     */
    private RouteTarget routeTarget;

    /**
     * Constructs an OutboundInvocation with a request and invocation context.
     *
     * @param request the request associated with this invocation
     * @param context the invocation context
     */
    public OutboundInvocation(T request, InvocationContext context) {
        super(request, context);
    }

    /**
     * Constructs an OutboundInvocation with a request and a base invocation.
     *
     * @param request    the request associated with this invocation
     * @param invocation the base invocation from which to derive properties
     */
    public OutboundInvocation(T request, Invocation<?> invocation) {
        this.request = request;
        this.context = invocation.getContext();
        this.governancePolicy = invocation.governancePolicy;
        this.liveMetadata = invocation.getLiveMetadata();
        this.laneMetadata = invocation.getLaneMetadata();
        ServiceParser serviceParser = createServiceParser();
        this.serviceMetadata = serviceParser.configure(serviceParser.parse(), liveMetadata == null ? null : liveMetadata.getRule());
    }

    @Override
    protected ServiceParser createServiceParser() {
        return new OutboundServiceMetadataParser(request, governancePolicy, context);
    }

    @Override
    protected LiveParser createLiveParser() {
        return new OutboundLiveMetadataParser(request, context.getGovernanceConfig().getLiveConfig(),
                context.getApplication(), governancePolicy);
    }

    @Override
    public void resetOnRetry() {
        listeners = null;
        routeTarget = null;
    }

    /**
     * Adds a {@link OutboundListener} to the list of listeners.
     *
     * @param listener the {@link OutboundListener} to add, if it is not null
     */
    public void addListener(OutboundListener listener) {
        if (listener != null) {
            if (listeners == null) {
                listeners = new ArrayList<>();
            }
            listeners.add(listener);
        }
    }

    /**
     * Handles the election process for a given endpoint.
     *
     * @param endpoint the endpoint to be elected.
     * @return true if the election process is successful and all listeners approve, false otherwise.
     */
    public boolean onElect(Endpoint endpoint) {
        if (!endpoint.predicate()) {
            return false;
        }
        if (listeners != null) {
            for (OutboundListener listener : listeners) {
                if (!listener.onElect(endpoint, this)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Handles the forwarding of an invocation to the specified endpoint.
     *
     * @param endpoint the endpoint to which the invocation is forwarded.
     */
    public void onForward(Endpoint endpoint) {
        if (endpoint != null) {
            request.addAttempt(endpoint.getId());
        }
        onForwardEvent(endpoint);
        if (listeners != null) {
            listeners.forEach(listener -> listener.onForward(endpoint, this));
        }
    }

    /**
     * Handles the successful completion of an invocation, notifying listeners with the response.
     *
     * @param endpoint the endpoint to which the invocation was sent.
     * @param response the response received from the service.
     */
    public void onSuccess(Endpoint endpoint, ServiceResponse response) {
        if (listeners != null) {
            listeners.forEach(listener -> listener.onSuccess(endpoint, this, response));
        }
    }

    /**
     * Handles the failure of an invocation, publishing a reject traffic event based on the type of exception
     * and notifying listeners.
     *
     * @param endpoint  the endpoint to which the invocation was sent.
     * @param throwable the exception that caused the failure.
     */
    public void onFailure(Endpoint endpoint, Throwable throwable) {
        if (listeners != null) {
            listeners.forEach(listener -> listener.onFailure(endpoint, this, throwable));
        }
    }

    /**
     * Request was recover successfully by degrade
     */
    public void onRecover() {
        onRejectEvent(RejectType.REJECT_DEGRADE);
    }

    /**
     * Returns the size of the endpoint collection based on the current state of instances and routeTarget.
     *
     * @return the size of the endpoint collection
     */
    public int getEndpointSize() {
        if (instances == null) {
            return 0;
        } else if (routeTarget == null) {
            return instances.size();
        } else {
            return routeTarget.getEndpoints().size();
        }
    }

    public boolean isEmpty() {
        return instances == null || instances.isEmpty();
    }

    /**
     * Retrieves the endpoints targeted by this outbound invocation.
     *
     * @return a list of endpoints, or an empty list if no route target is set.
     */
    public List<? extends Endpoint> getEndpoints() {
        return routeTarget == null ? new ArrayList<>(0) : routeTarget.getEndpoints();
    }

    public Endpoint getEndpoint() {
        if (routeTarget == null) {
            return null;
        }
        List<? extends Endpoint> endpoints = routeTarget.getEndpoints();
        return endpoints == null || endpoints.isEmpty() ? null : endpoints.get(0);
    }

    /**
     * Get the routing target. When the routing target is empty, the default routing target is returned.
     *
     * @return RouteTarget
     */
    public RouteTarget getRouteTarget() {
        if (null == routeTarget) {
            if (instances == null) {
                routeTarget = RouteTarget.forward(new ArrayList<>());
            } else {
                // use array list to improve performance.
                routeTarget = RouteTarget.forward(new ArrayList<>(instances));
            }
        }
        return routeTarget;
    }

    /**
     * Handles a forward event.
     */
    protected void onForwardEvent(Endpoint endpoint) {
        publish(TrafficEvent.builder().actionType(TrafficEvent.ActionType.FORWARD).requests(1), endpoint);
    }

    /**
     * Publishes a live event to a specified publisher using a configured live event builder.
     *
     * @param builder  The live event builder used to configure and build the live event. If {@code null}, no action is taken.
     * @param endpoint The endpoint associated with the live event. This parameter is currently unused in the method.
     */
    protected void publish(TrafficEventBuilder builder, Endpoint endpoint) {
        if (builder != null) {
            TrafficEvent event = configure(builder, endpoint).build();
            if (event != null) {
                context.publish(event);
            }
        }
    }

    @Override
    protected TrafficEventBuilder configure(TrafficEventBuilder builder) {
        return super.configure(builder).componentType(ComponentType.SERVICE).direction(Direction.OUTBOUND);
    }

    /**
     * Configures a live event builder with details from the current invocation context and associated metadata.
     * This method populates the builder with information such as live space, unit rule, local unit, local cell,
     * lane space, local lane, target lane, policy ID, and service-related details extracted from the URI.
     * If any metadata or URI is null, the corresponding fields in the builder are set to null.
     *
     * @param builder  The live event builder to configure. Must not be null.
     * @param endpoint The endpoint associated with the current invocation. Used to provide additional context.
     * @return The configured live event builder, populated with relevant details from the invocation context.
     */
    protected TrafficEventBuilder configure(TrafficEventBuilder builder, Endpoint endpoint) {
        return configure(builder)
                .targetLane(endpoint == null ? null : endpoint.getLane())
                .targetUnit(endpoint == null ? null : endpoint.getUnit())
                .targetCell(endpoint == null ? null : endpoint.getCell());
    }

    /**
     * A specialized static inner class representing an RPC outbound invocation. This class
     * extends the OutboundInvocation to provide RPC-specific logic for handling outbound requests.
     *
     * @param <T> the type parameter of the RPC outbound request, which must extend RpcOutboundRequest
     */
    public static class RpcOutboundInvocation<T extends RpcOutboundRequest> extends OutboundInvocation<T> {

        public RpcOutboundInvocation(T request, InvocationContext context) {
            super(request, context);
        }

        public RpcOutboundInvocation(T request, Invocation<?> invocation) {
            super(request, invocation);
        }

        @Override
        protected LiveParser createLiveParser() {
            return new RpcOutboundLiveMetadataParser(request, context.getGovernanceConfig().getLiveConfig(),
                    context.getApplication(), governancePolicy, context::getVariableParser);
        }
    }

    /**
     * A specialized static inner class representing an HTTP outbound invocation. This class
     * extends the OutboundInvocation to provide HTTP-specific handling for outbound requests.
     *
     * @param <T> the type parameter of the HTTP outbound request, which must extend HttpOutboundRequest
     */
    public static class HttpOutboundInvocation<T extends HttpOutboundRequest> extends OutboundInvocation<T> {

        public HttpOutboundInvocation(T request, InvocationContext context) {
            super(request, context);
        }

        public HttpOutboundInvocation(T request, Invocation<?> invocation) {
            super(request, invocation);
        }

    }

    /**
     * A specialized implementation of {@link HttpOutboundInvocation} for handling HTTP forwarding requests.
     *
     * @param <T> the type of HTTP outbound request, which must extend {@link HttpOutboundRequest}
     */
    public static class HttpForwardInvocation<T extends HttpOutboundRequest> extends HttpOutboundInvocation<T> {

        public HttpForwardInvocation(T request, InvocationContext context) {
            super(request, context);
        }

        public HttpForwardInvocation(T request, Invocation<?> invocation) {
            super(request, invocation);
        }

        @Override
        protected ServiceParser createServiceParser() {
            return new ForwardServiceMetadataParser(context.getGovernanceConfig().getServiceConfig());
        }
    }

    /**
     * A specialized static inner class representing an HTTP outbound invocation that is specifically
     * designed for use in a gateway scenario. This class extends the HttpOutboundInvocation to provide
     * additional gateway-specific logic for handling outbound requests.
     *
     * @param <T> the type parameter of the HTTP outbound request, which must extend HttpOutboundRequest
     */
    public static class GatewayHttpOutboundInvocation<T extends HttpOutboundRequest> extends HttpOutboundInvocation<T> {

        public GatewayHttpOutboundInvocation(T request, InvocationContext context) {
            super(request, context);
        }

        public GatewayHttpOutboundInvocation(T request, Invocation<?> invocation) {
            super(request, invocation);
        }

        @Override
        public GatewayRole getGateway() {
            return context.getGatewayRole() == GatewayRole.FRONTEND ? GatewayRole.FRONTEND : GatewayRole.BACKEND;
        }

        @Override
        protected ServiceParser createServiceParser() {
            return new GatewayOutboundServiceMetadataParser(request, governancePolicy, context);
        }

        @Override
        protected TrafficEventBuilder configure(TrafficEventBuilder builder) {
            return super.configure(builder).componentType(context.getGatewayRole() == GatewayRole.FRONTEND
                    ? ComponentType.FRONTEND_GATEWAY
                    : ComponentType.BACKEND_GATEWAY);
        }
    }

    /**
     * A specialized static inner class representing an RPC outbound invocation designed for use in a gateway scenario.
     * This class extends the RpcOutboundInvocation to provide gateway-specific logic for handling outbound RPC requests.
     *
     * @param <T> the type parameter of the RPC outbound request, which must extend RpcOutboundRequest
     */
    public static class GatewayRpcOutboundInvocation<T extends RpcOutboundRequest> extends RpcOutboundInvocation<T> {

        public GatewayRpcOutboundInvocation(T request, Invocation<?> invocation) {
            super(request, invocation);
        }

        @Override
        public GatewayRole getGateway() {
            return context.getGatewayRole() == GatewayRole.FRONTEND ? GatewayRole.FRONTEND : GatewayRole.BACKEND;
        }

        @Override
        protected ServiceParser createServiceParser() {
            return new GatewayOutboundServiceMetadataParser(request, governancePolicy, context);
        }

        @Override
        protected TrafficEventBuilder configure(TrafficEventBuilder builder) {
            return super.configure(builder).componentType(context.getGatewayRole() == GatewayRole.FRONTEND
                    ? ComponentType.FRONTEND_GATEWAY
                    : ComponentType.BACKEND_GATEWAY);
        }
    }

    /**
     * A specialized implementation of {@link HttpOutboundInvocation} for handling HTTP forwarding requests in a gateway context.
     * This class extends {@link HttpOutboundInvocation} and provides additional functionality specific to gateway roles,
     * service metadata parsing, and traffic event configuration.
     *
     * @param <T> the type of HTTP outbound request, which must extend {@link HttpOutboundRequest}
     */
    public static class GatewayHttpForwardInvocation<T extends HttpOutboundRequest> extends HttpForwardInvocation<T> {

        public GatewayHttpForwardInvocation(T request, InvocationContext context) {
            super(request, context);
        }

        public GatewayHttpForwardInvocation(T request, Invocation<?> invocation) {
            super(request, invocation);
        }

        @Override
        public GatewayRole getGateway() {
            return context.getGatewayRole() == GatewayRole.FRONTEND ? GatewayRole.FRONTEND : GatewayRole.BACKEND;
        }

        @Override
        protected TrafficEventBuilder configure(TrafficEventBuilder builder, Endpoint endpoint) {
            return super.configure(builder).componentType(context.getGatewayRole() == GatewayRole.FRONTEND
                    ? ComponentType.FRONTEND_GATEWAY
                    : ComponentType.BACKEND_GATEWAY);
        }
    }
}
