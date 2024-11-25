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

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.instance.GatewayRole;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.event.TrafficEvent.ActionType;
import com.jd.live.agent.governance.event.TrafficEvent.ComponentType;
import com.jd.live.agent.governance.event.TrafficEvent.Direction;
import com.jd.live.agent.governance.event.TrafficEvent.TrafficEventBuilder;
import com.jd.live.agent.governance.invoke.metadata.LiveDomainMetadata;
import com.jd.live.agent.governance.invoke.metadata.parser.LaneMetadataParser.GatewayInboundLaneMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.LaneMetadataParser.HttpInboundLaneMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.LiveMetadataParser.GatewayInboundLiveMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.LiveMetadataParser.HttpInboundLiveMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.LiveMetadataParser.InboundLiveMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.LaneParser;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.LiveParser;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.ServiceParser;
import com.jd.live.agent.governance.invoke.metadata.parser.ServiceMetadataParser.GatewayInboundServiceMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.ServiceMetadataParser.InboundServiceMetadataParser;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.domain.Domain;
import com.jd.live.agent.governance.policy.domain.DomainPolicy;
import com.jd.live.agent.governance.policy.live.Place;
import com.jd.live.agent.governance.request.HttpRequest.HttpInboundRequest;
import com.jd.live.agent.governance.request.RpcRequest.RpcInboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that represents an inbound invocation of a service with a request of type T.
 * It extends the basic Invocation class and provides additional functionality specific to
 * inbound operations.
 *
 * @param <T> the type parameter of the inbound request
 */
public abstract class InboundInvocation<T extends InboundRequest> extends Invocation<T> {

    /**
     * The action to be performed at the unit level.
     */
    @Setter
    @Getter
    protected UnitAction unitAction;

    /**
     * The action to be performed at the cell level.
     */
    @Setter
    @Getter
    protected CellAction cellAction;

    protected List<InboundListener> listeners;

    /**
     * Constructs an InboundInvocation with the specified request and context.
     *
     * @param request the inbound request
     * @param context the invocation context
     */
    public InboundInvocation(T request, InvocationContext context) {
        super(request, context);
    }

    @Override
    protected ServiceParser createServiceParser() {
        return new InboundServiceMetadataParser(request, context.getGovernanceConfig().getServiceConfig(),
                context.getApplication(), governancePolicy);
    }

    @Override
    protected LiveParser createLiveParser() {
        return new InboundLiveMetadataParser(request, context.getGovernanceConfig().getLiveConfig(),
                context.getApplication(), governancePolicy);
    }

    @Override
    protected TrafficEventBuilder configure(TrafficEventBuilder builder) {
        return super.configure(builder).componentType(ComponentType.SERVICE).direction(Direction.INBOUND);
    }

    /**
     * Adds a {@link InboundListener} to the list of listeners.
     *
     * @param listener the {@link InboundListener} to add, if it is not null
     */
    public void addListener(InboundListener listener) {
        if (listener != null) {
            if (listeners == null) {
                listeners = new ArrayList<>();
            }
            listeners.add(listener);
        }
    }

    /**
     * Handles the forwarding of an invocation, publishing a forward traffic event and notifying listeners.
     */
    public void onForward() {
        publish(context.getTrafficPublisher(), TrafficEvent.builder().actionType(ActionType.FORWARD).requests(1));
        if (listeners != null) {
            listeners.forEach(listener -> listener.onForward(this));
        }
    }

    /**
     * Handles the failure of an invocation, publishing a reject traffic event based on the type of exception
     * and notifying listeners.
     *
     * @param throwable the exception that caused the failure.
     */
    public void onFailure(Throwable throwable) {
        if (listeners != null) {
            listeners.forEach(listener -> listener.onFailure(this, throwable));
        }
    }

    /**
     * A specialized static inner class representing an RPC inbound invocation. This class
     * is designed to handle RPC-specific logic within the context of an inbound service
     * invocation.
     *
     * @param <T> the type parameter of the RPC inbound request, which must extend RpcInboundRequest
     */
    public static class RpcInboundInvocation<T extends RpcInboundRequest> extends InboundInvocation<T> {

        public RpcInboundInvocation(T request, InvocationContext context) {
            super(request, context);
        }

    }

    /**
     * A specialized static inner class representing an HTTP inbound invocation. This class
     * extends the functionality of InboundInvocation to handle HTTP-specific logic and policies.
     *
     * @param <T> the type parameter of the HTTP inbound request, which must extend HttpInboundRequest
     */
    public static class HttpInboundInvocation<T extends HttpInboundRequest> extends InboundInvocation<T> {

        protected DomainPolicy domainPolicy;

        public HttpInboundInvocation(T request, InvocationContext context) {
            super(request, context);
        }

        @Override
        protected void parsePolicy() {
            Domain domain = governancePolicy == null ? null : governancePolicy.getDomain(request.getHost());
            domainPolicy = domain == null ? null : domain.getPolicy();
            super.parsePolicy();
        }

        @Override
        protected LiveParser createLiveParser() {
            return new HttpInboundLiveMetadataParser(request, context.getGovernanceConfig().getLiveConfig(),
                    context.getApplication(), governancePolicy,
                    context::getVariableParser, context::getVariableFunction, domainPolicy);
        }

        @Override
        protected LaneParser createLaneParser() {
            return new HttpInboundLaneMetadataParser(request, context.getGovernanceConfig().getLaneConfig(),
                    context.getApplication(), governancePolicy, domainPolicy, this);
        }

        @Override
        protected PolicyId parsePolicyId() {
            if (domainPolicy != null && liveMetadata != null) {
                return ((LiveDomainMetadata) liveMetadata).getPolicyId();
            }
            return super.parsePolicyId();
        }

        @Override
        protected TrafficEventBuilder configure(TrafficEventBuilder builder) {
            TrafficEventBuilder result = super.configure(builder);
            return liveMetadata == null ? result : result.variable(((LiveDomainMetadata) liveMetadata).getBizVariable());
        }
    }

    /**
     * A specialized static inner class representing an inbound invocation at a gateway. This class
     * extends the HttpInboundInvocation to provide gateway-specific logic and handling of HTTP requests.
     *
     * @param <T> the type parameter of the HTTP inbound request, which must extend HttpInboundRequest
     */
    public static class GatewayInboundInvocation<T extends HttpInboundRequest> extends HttpInboundInvocation<T> {

        public GatewayInboundInvocation(T request, InvocationContext context) {
            super(request, context);
        }

        @Override
        public GatewayRole getGateway() {
            if (GatewayRole.FRONTEND == context.getApplication().getService().getGateway()) {
                return GatewayRole.FRONTEND;
            }
            return GatewayRole.BACKEND;
        }

        @Override
        protected void parsePolicy() {
            if (context.getApplication().getService().isFrontGateway()) {
                // remove rule id at frontend gateway
                Carrier carrier = RequestContext.get();
                if (carrier != null) {
                    carrier.removeCargo(Constants.LABEL_LIVE_SPACE_ID);
                    carrier.removeCargo(Constants.LABEL_RULE_ID);
                    carrier.removeCargo(Constants.LABEL_VARIABLE);
                    carrier.removeCargo(Constants.LABEL_LANE_SPACE_ID);
                    carrier.removeCargo(Constants.LABEL_LANE);
                }
            }
            super.parsePolicy();
        }

        @Override
        protected LaneParser createLaneParser() {
            return new GatewayInboundLaneMetadataParser(request, context.getGovernanceConfig().getLaneConfig(),
                    context.getApplication(), governancePolicy, domainPolicy, this);
        }

        @Override
        protected LiveParser createLiveParser() {
            return new GatewayInboundLiveMetadataParser(request, context.getGovernanceConfig().getLiveConfig(),
                    context.getApplication(), governancePolicy,
                    context::getVariableParser, context::getVariableFunction, domainPolicy);
        }

        @Override
        protected ServiceParser createServiceParser() {
            return new GatewayInboundServiceMetadataParser(request, context.getGovernanceConfig().getServiceConfig(),
                    context.getApplication(), governancePolicy);
        }

        @Override
        public boolean isAccessible(Place place) {
            // Accept incoming requests and then forward them to the correct unit.
            // TODO why ?
            return place != null && (place == liveMetadata.getLocalUnit() || place == liveMetadata.getLocalCell() || super.isAccessible(place));
        }

        @Override
        protected TrafficEventBuilder configure(TrafficEventBuilder builder) {
            return super.configure(builder).componentType(ComponentType.GATEWAY);
        }
    }
}
