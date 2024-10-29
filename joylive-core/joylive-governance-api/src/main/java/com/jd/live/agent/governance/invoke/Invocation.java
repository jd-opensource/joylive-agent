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
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.instance.GatewayRole;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.matcher.Matcher;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.event.TrafficEvent.TrafficEventBuilder;
import com.jd.live.agent.governance.invoke.matcher.TagMatcher;
import com.jd.live.agent.governance.invoke.metadata.LaneMetadata;
import com.jd.live.agent.governance.invoke.metadata.LiveMetadata;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
import com.jd.live.agent.governance.invoke.metadata.parser.LaneMetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.LaneParser;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.LiveParser;
import com.jd.live.agent.governance.invoke.metadata.parser.MetadataParser.ServiceParser;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.lane.Lane;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.rule.tag.TagCondition;
import lombok.Getter;

import java.util.Map;

/**
 * Abstract class for an invocation, encapsulating the context and metadata required for processing a service request.
 * <p>
 * This class provides a common foundation for handling various aspects of a service request, such as governance policies,
 * unit functions, variable functions, and service configuration. It also offers utility methods for matching tags,
 * accessing governance configurations, and dealing with faults and failovers.
 *
 * @param <T> the type of service request this invocation is handling
 */
public abstract class Invocation<T extends ServiceRequest> implements Matcher<TagCondition> {

    public static final String REJECT_NAMESPACE_NOT_MATCH = "reject when namespace is not matched.";
    public static final String FAILOVER_UNIT_NOT_ACCESSIBLE = "failover when unit is not accessible.";
    public static final String REJECT_NO_UNIT = "reject when local unit is not found.";
    public static final String REJECT_UNIT_NOT_CENTER = "reject when unit is not center.";
    public static final String REJECT_NO_CENTER = "reject when center unit is not found.";
    public static final String REJECT_UNIT_NOT_ACCESSIBLE = "reject when unit is not accessible.";
    public static final String REJECT_NO_VARIABLE = "reject when unit variable is not found.";
    public static final String REJECT_NO_UNIT_ROUTE = "reject when unit route is not found.";
    public static final String FAILOVER_CENTER_NO_VARIABLE = "failover center unit when unit variable is not found.";
    public static final String FAILOVER_UNIT_ESCAPE = "failover unit when variable is not belong to this unit.";
    public static final String FAILOVER_CELL_ESCAPE = "failover cell when variable is not belong to this cell.";
    public static final String FAILOVER_CELL_NOT_ACCESSIBLE = "failover other cell when local cell is not accessible.";

    /**
     * The service request associated with this invocation.
     */
    @Getter
    protected T request;

    @Getter
    protected InvocationContext context;

    @Getter
    protected GovernancePolicy governancePolicy;

    @Getter
    protected ServiceMetadata serviceMetadata;

    @Getter
    protected LiveMetadata liveMetadata;

    @Getter
    protected LaneMetadata laneMetadata;

    /**
     * The policy id for this invocation
     */
    protected PolicyId policyId;

    /**
     * Constructs a new Invocation object.
     */
    protected Invocation() {
    }

    /**
     * Constructs a new Invocation object with a specific request and invocation context.
     *
     * @param request the service request
     * @param context the invocation context
     */
    public Invocation(T request, InvocationContext context) {
        this.request = request;
        this.context = context;
        this.governancePolicy = context.getPolicySupplier().getPolicy();
        parsePolicy();
    }

    /**
     * Parses and configures the policy metadata.
     * <p>
     * This method creates instances of {@link ServiceParser}, {@link LiveParser}, and {@link MetadataParser}
     * for parsing and configuring the service metadata, live metadata, and lane metadata, respectively.
     * The parsed and configured metadata is stored in the corresponding instance variables of the class.
     * </p>
     */
    protected void parsePolicy() {
        ServiceParser serviceParser = createServiceParser();
        LiveParser liveParser = createLiveParser();
        MetadataParser<LaneMetadata> laneParser = createLaneParser();
        ServiceMetadata serviceMetadata = serviceParser.parse();
        LiveMetadata liveMetadata = liveParser.parse();
        this.serviceMetadata = serviceParser.configure(serviceMetadata, liveMetadata.getRule());
        this.liveMetadata = liveParser.configure(liveMetadata, serviceMetadata.getServicePolicy());
        this.laneMetadata = laneParser.parse();
        this.policyId = parsePolicyId();
    }

    /**
     * Creates an instance of {@link LiveParser} for parsing live metadata.
     *
     * @return the created {@link LiveParser} instance
     */
    protected abstract LiveParser createLiveParser();

    /**
     * Creates an instance of {@link ServiceParser} for parsing service metadata.
     *
     * @return the created {@link ServiceParser} instance
     */
    protected abstract ServiceParser createServiceParser();

    /**
     * Creates an instance of {@link LaneParser} for parsing lane metadata.
     *
     * @return the created {@link LaneParser} instance
     */
    protected LaneParser createLaneParser() {
        return new LaneMetadataParser(request, context.getGovernanceConfig().getLaneConfig(),
                context.getApplication(), governancePolicy);
    }

    /**
     * Parses the policy ID from the service metadata.
     *
     * @return the parsed policy ID
     */
    protected PolicyId parsePolicyId() {
        return serviceMetadata.getServicePolicy();
    }

    public GatewayRole getGateway() {
        return GatewayRole.NONE;
    }

    /**
     * Resets the state of the instance.
     * This method is typically used to restore the instance to its initial state.
     */
    public void reset() {

    }

    /**
     * Determines if a place is accessible based on the current request's write status and the place's access mode.
     *
     * @param place The place to check for accessibility.
     * @return true if the place is accessible, false otherwise.
     */
    public boolean isAccessible(Place place) {
        return place != null && isAccessible(place.getAccessMode());
    }

    /**
     * Checks if the current cell is accessible for the given {@code AccessMode}.
     *
     * @param accessMode The desired access mode. If null, it defaults to {@link AccessMode#READ_WRITE}.
     * @return true if the cell is accessible for the given access mode, false otherwise.
     */
    public boolean isAccessible(AccessMode accessMode) {
        accessMode = accessMode == null ? AccessMode.READ_WRITE : accessMode;
        switch (accessMode) {
            case READ_WRITE:
                return true;
            case READ:
                return !serviceMetadata.isWrite();
        }
        return false;
    }

    /**
     * Matches a tag condition against the request.
     *
     * @param condition The tag condition to match.
     * @return true if the condition matches the request, false otherwise.
     */
    public boolean match(TagCondition condition) {
        if (condition == null) {
            return true;
        } else if (request == null) {
            return false;
        }
        Map<String, TagMatcher> matchers = context.getTagMatchers();
        TagMatcher matcher = matchers == null ? null : matchers.get(condition.getType().toLowerCase());
        return matcher != null && matcher.match(condition, request);
    }

    /**
     * Rejects the request with a specified fault type and reason.
     *
     * @param type   The type of fault.
     * @param reason The reason for the fault.
     */
    public void reject(FaultType type, String reason) {
        request.reject(type, reason);
    }

    /**
     * Initiates a failover for the request with a specified fault type and reason.
     *
     * @param type   The type of fault.
     * @param reason The reason for the failover.
     */
    public void failover(FaultType type, String reason) {
        request.failover(type, reason);
    }

    /**
     * Initiates a degradation for the request with a specified fault type and reason.
     *
     * @param type   The type of fault.
     * @param reason The reason for the failover.
     * @param config The degrade config.
     */
    public void degrade(FaultType type, String reason, DegradeConfig config) {
        request.degrade(type, reason, config);
    }

    /**
     * Handles a reject event by publishing a traffic event with the appropriate reject type.
     *
     * @param exception the reject exception that occurred.
     */
    public void onReject(RejectException exception) {
        if (exception instanceof RejectUnreadyException) {
            publish(context.getTrafficPublisher(), TrafficEvent.builder().actionType(TrafficEvent.ActionType.REJECT).rejectType(TrafficEvent.RejectType.REJECT_UNREADY).requests(1));
        } else if (exception instanceof RejectUnitException) {
            publish(context.getTrafficPublisher(), TrafficEvent.builder().actionType(TrafficEvent.ActionType.REJECT).rejectType(TrafficEvent.RejectType.REJECT_UNIT_UNAVAILABLE).requests(1));
        } else if (exception instanceof RejectCellException) {
            publish(context.getTrafficPublisher(), TrafficEvent.builder().actionType(TrafficEvent.ActionType.REJECT).rejectType(TrafficEvent.RejectType.REJECT_CELL_UNAVAILABLE).requests(1));
        } else if (exception instanceof RejectEscapeException) {
            publish(context.getTrafficPublisher(), TrafficEvent.builder().actionType(TrafficEvent.ActionType.REJECT).rejectType(TrafficEvent.RejectType.REJECT_ESCAPE).requests(1));
        } else if (exception instanceof RejectLimitException) {
            publish(context.getTrafficPublisher(), TrafficEvent.builder().actionType(TrafficEvent.ActionType.REJECT).rejectType(TrafficEvent.RejectType.REJECT_LIMIT).requests(1));
        } else if (exception instanceof RejectPermissionException) {
            publish(context.getTrafficPublisher(), TrafficEvent.builder().actionType(TrafficEvent.ActionType.REJECT).rejectType(TrafficEvent.RejectType.REJECT_PERMISSION_DENIED).requests(1));
        } else if (exception instanceof RejectAuthException) {
            publish(context.getTrafficPublisher(), TrafficEvent.builder().actionType(TrafficEvent.ActionType.REJECT).rejectType(TrafficEvent.RejectType.REJECT_UNAUTHORIZED).requests(1));
        }
    }

    /**
     * Publishes a live event to a specified publisher using a configured live event builder.
     *
     * @param publisher The publisher to which the live event will be offered.
     * @param builder   The live event builder used to configure and build the live event.
     */
    public void publish(Publisher<TrafficEvent> publisher, TrafficEventBuilder builder) {
        if (publisher != null && builder != null) {
            TrafficEvent event = configure(builder).build();
            if (event != null) {
                publisher.tryOffer(event);
            }
        }
    }

    /**
     * Configures a live event builder with details from the current invocation context.
     *
     * @param builder The live event builder to configure.
     * @return The configured live event builder.
     */
    protected TrafficEventBuilder configure(TrafficEventBuilder builder) {
        LiveSpace liveSpace = liveMetadata.getTargetSpace();
        UnitRule unitRule = liveMetadata.getRule();
        Unit localUnit = liveMetadata.getLocalUnit();
        Cell localCell = liveMetadata.getLocalCell();
        LaneSpace laneSpace = laneMetadata.getLaneSpace();
        Lane localLane = laneMetadata.getCurrentLane();
        Lane targetLane = laneMetadata.getTargetLane();
        URI uri = policyId == null ? null : policyId.getUri();
        return builder.liveSpaceId(liveSpace == null ? null : liveSpace.getId()).
                unitRuleId(unitRule == null ? null : unitRule.getId()).
                localUnit(localUnit == null ? null : localUnit.getCode()).
                localCell(localCell == null ? null : localCell.getCode()).
                laneSpaceId(laneSpace == null ? null : laneSpace.getId()).
                localLane(localLane == null ? null : localLane.getCode()).
                targetLane(targetLane == null ? null : targetLane.getCode()).
                policyId(policyId == null ? null : policyId.getId()).
                service(uri == null ? null : uri.getHost()).
                group(uri == null ? null : uri.getParameter(PolicyId.KEY_SERVICE_GROUP)).
                path(uri == null ? null : uri.getPath()).
                method(uri == null ? null : uri.getParameter(PolicyId.KEY_SERVICE_METHOD));
    }

    /**
     * Constructs an error message incorporating additional details from the invocation context.
     * This method overloads {@link #getError(String, String, String)} by using the application's location.
     *
     * @param message The base error message.
     * @return The constructed error message with additional context details.
     */
    public String getError(String message) {
        return getError(message, context.getApplication().getLocation());
    }

    /**
     * Constructs an error message incorporating additional details from the specified location.
     * This method overloads {@link #getError(String, String, String)} by extracting unit and cell from the given location.
     *
     * @param message  The base error message.
     * @param location The location to include in the error message.
     * @return The constructed error message with additional context details.
     */
    public String getError(String message, Location location) {
        return getError(message, location.getUnit(), location.getCell());
    }

    /**
     * Constructs an error message incorporating additional details using the specified unit.
     * This method overloads {@link #getError(String, String, String)} by allowing a null cell value.
     *
     * @param message The base error message.
     * @param unit    The unit to include in the error message.
     * @return The constructed error message with additional context details.
     */
    public String getError(String message, String unit) {
        return getError(message, unit, null);
    }

    /**
     * Constructs an error message incorporating detailed context information from the invocation.
     * This method appends various details such as live space ID, rule ID, unit, cell, application name,
     * service name, service group, request path, and variable to the base error message.
     *
     * @param message The base error message.
     * @param unit    The unit associated with the error, may be null.
     * @param cell    The cell associated with the error, may be null.
     * @return The constructed error message with detailed context information.
     */
    public String getError(String message, String unit, String cell) {
        LiveSpace liveSpace = liveMetadata.getTargetSpace();
        return new StringBuilder(message.length() + 150).append(message).
                append(". liveSpaceId=").append(liveSpace == null ? null : liveSpace.getId()).
                append(", ruleId=").append(liveMetadata.getRuleId()).
                append(", unit=").append(unit).
                append(", cell=").append(cell).
                append(", application=").append(context.getApplication().getName()).
                append(", service=").append(serviceMetadata.getServiceName()).
                append(", group=").append(serviceMetadata.getServiceGroup()).
                append(", path=").append(serviceMetadata.getPath()).
                append(", variable=").append(liveMetadata.getVariable()).append("\n").toString();
    }
}
