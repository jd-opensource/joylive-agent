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
package com.jd.live.agent.governance.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * The TrafficEvent class represents a traffic event, which captures various details
 * about network traffic for monitoring, logging, or analysis purposes.
 */
@Getter
@Builder
@AllArgsConstructor
public class TrafficEvent {

    /**
     * The type of component that generated the traffic event.
     */
    private final ComponentType componentType;

    /**
     * The direction of the traffic with respect to the component.
     */
    private final Direction direction;

    /**
     * The ID of the live space associated with the event.
     */
    private final String liveSpaceId;

    /**
     * The ID of the unit rule associated with the event.
     */
    private final String unitRuleId;

    /**
     * The local unit involved in the traffic event.
     */
    private final String localUnit;

    /**
     * The local cell within the network where the event occurred.
     */
    private final String localCell;

    /**
     * The local lane within the network where the event occurred.
     */
    private final String localLane;

    /**
     * The target unit of the traffic event.
     */
    private final String targetUnit;

    /**
     * The target cell where the traffic is destined.
     */
    private final String targetCell;

    private final String liveDomain;

    private final String livePath;

    private final String liveBizVariable;

    /**
     * The ID of the lane space associated with the event.
     */
    private final String laneSpaceId;

    /**
     * The ID of the lane rule associated with the event.
     */
    private final String laneRuleId;

    /**
     * The target lane where the traffic is destined.
     */
    private final String targetLane;

    /**
     * The service that is involved in the traffic event.
     */
    private final String service;

    /**
     * The group to which the traffic event belongs.
     */
    private final String group;

    /**
     * The path associated with the traffic event.
     */
    private final String path;

    /**
     * The method that was called or is associated with the traffic event.
     */
    private final String method;

    /**
     * The unique id of policy uri which representing the path where the policy takes effect. such as: <br/>
     * <li>service://service</li>
     * <li>service://service?group=yyyy</li>
     * <li>service://service/path?group=yyyy</li>
     * <li>service://service/path?group=yyyy&method=xxx</li>
     * <li>gateway://domain/</li>
     * <li>gateway://domain/path</li>
     * <li>gateway://domain/path?variable=xxxx</li>
     */
    private final Long policyId;

    /**
     * A map of policy tags that are associated with the traffic event.
     */
    private final Map<String, String> policyTags;

    /**
     * The action taken or to be taken for the traffic event, such as FORWARD or REJECT.
     */
    private final ActionType actionType;

    private final RejectType rejectType;

    /**
     * The number of requests associated with the traffic event.
     */
    private final int requests;

    public String getRejectTypeName() {
        return rejectType == null ? null : rejectType.name();
    }

    public static TrafficEventBuilder builder() {
        return new TrafficEventBuilder();
    }

    /**
     * An enumeration representing the type of component that generates the traffic event.
     */
    public enum ComponentType {

        /**
         * Represents a network frontend gateway component.
         */
        FRONTEND_GATEWAY,

        /**
         * Represents a network backend gateway component.
         */
        BACKEND_GATEWAY,

        /**
         * Represents a service component.
         */
        SERVICE;

        public boolean isGateway() {
            return this == FRONTEND_GATEWAY || this == BACKEND_GATEWAY;
        }
    }

    /**
     * An enumeration representing the direction of the traffic with respect to the component.
     */
    public enum Direction {
        /**
         * Represents traffic that is incoming to the component.
         */
        INBOUND,

        /**
         * Represents traffic that is outgoing from the component.
         */
        OUTBOUND
    }

    /**
     * An enumeration representing the action taken or to be taken for the traffic event.
     */
    public enum ActionType {
        /**
         * Represents an action to forward the traffic.
         */
        FORWARD,

        /**
         * Represents an action to reject the traffic.
         */
        REJECT
    }

    /**
     * An enumeration representing the reject type for the traffic event.
     */
    public enum RejectType {

        /**
         * No action is taken for the traffic event.
         */
        NONE,

        /**
         * The traffic event is rejected because the unit is unavailable.
         */
        REJECT_UNIT_UNAVAILABLE,

        /**
         * The traffic event is rejected because the cell is unavailable.
         */
        REJECT_CELL_UNAVAILABLE,

        /**
         * The traffic event is rejected due to an escape condition.
         */
        REJECT_ESCAPE,

        /**
         * The traffic event is rejected because the application is unready.
         */
        REJECT_UNREADY,

        /**
         * The traffic event is rejected because there is no available provider to handle the request.
         */
        REJECT_NO_PROVIDER,

        /**
         * The traffic event is rejected because the permission is denied.
         */
        REJECT_PERMISSION_DENIED,

        /**
         * The traffic event is rejected because the request is unauthorized.
         */
        REJECT_UNAUTHORIZED,

        /**
         * The traffic event is rejected because it has reached the maximum number of active requests.
         */
        REJECT_LIMIT,

        /**
         * The traffic event is rejected because the circuit breaker has been triggered.
         */
        REJECT_CIRCUIT_BREAK,

        /**
         * The traffic event is rejected because degrade has been triggered.
         */
        REJECT_DEGRADE,
    }

    public static class TrafficEventBuilder {

    }

}
