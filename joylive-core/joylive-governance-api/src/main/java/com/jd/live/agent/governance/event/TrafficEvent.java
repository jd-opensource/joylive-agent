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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * The TrafficEvent class represents a traffic event, which captures various details
 * about network traffic for monitoring, logging, or analysis purposes.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TrafficEvent {

    /**
     * The type of component that generated the traffic event.
     */
    private ComponentType componentType;

    /**
     * The direction of the traffic with respect to the component.
     */
    private Direction direction;

    /**
     * The ID of the live space associated with the event.
     */
    private String liveSpaceId;

    /**
     * The ID of the unit rule associated with the event.
     */
    private String unitRuleId;

    /**
     * The local unit involved in the traffic event.
     */
    private String localUnit;

    /**
     * The local cell within the network where the event occurred.
     */
    private String localCell;

    /**
     * The local lane within the network where the event occurred.
     */
    private String localLane;

    /**
     * The target unit of the traffic event.
     */
    private String targetUnit;

    /**
     * The target cell where the traffic is destined.
     */
    private String targetCell;

    private String liveDomain;

    private String livePath;

    private String liveBizVariable;

    /**
     * The ID of the lane space associated with the event.
     */
    private String laneSpaceId;

    /**
     * The ID of the lane rule associated with the event.
     */
    private String laneRuleId;

    /**
     * The target lane where the traffic is destined.
     */
    private String targetLane;

    /**
     * The service that is involved in the traffic event.
     */
    private String service;

    /**
     * The group to which the traffic event belongs.
     */
    private String group;

    /**
     * The path associated with the traffic event.
     */
    private String path;

    /**
     * The method that was called or is associated with the traffic event.
     */
    private String method;

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
    private Long policyId;

    /**
     * A map of policy tags that are associated with the traffic event.
     */
    private Map<String, String> policyTags;

    /**
     * The action taken or to be taken for the traffic event, such as FORWARD or REJECT.
     */
    private ActionType actionType;

    private RejectType rejectType;

    /**
     * The number of requests associated with the traffic event.
     */
    private int requests;

    public TrafficEvent componentType(ComponentType componentType) {
        this.componentType = componentType;
        return this;
    }

    public TrafficEvent direction(Direction direction) {
        this.direction = direction;
        return this;
    }

    public TrafficEvent liveSpaceId(String liveSpaceId) {
        this.liveSpaceId = liveSpaceId;
        return this;
    }

    public TrafficEvent unitRuleId(String unitRuleId) {
        this.unitRuleId = unitRuleId;
        return this;
    }

    public TrafficEvent localUnit(String localUnit) {
        this.localUnit = localUnit;
        return this;
    }

    public TrafficEvent localCell(String localCell) {
        this.localCell = localCell;
        return this;
    }

    public TrafficEvent localLane(String localLane) {
        this.localLane = localLane;
        return this;
    }

    public TrafficEvent targetUnit(String targetUnit) {
        this.targetUnit = targetUnit;
        return this;
    }

    public TrafficEvent targetCell(String targetCell) {
        this.targetCell = targetCell;
        return this;
    }

    public TrafficEvent liveDomain(String liveDomain) {
        this.liveDomain = liveDomain;
        return this;
    }

    public TrafficEvent livePath(String livePath) {
        this.livePath = livePath;
        return this;
    }

    public TrafficEvent liveBizVariable(String liveBizVariable) {
        this.liveBizVariable = liveBizVariable;
        return this;
    }

    public TrafficEvent laneSpaceId(String laneSpaceId) {
        this.laneSpaceId = laneSpaceId;
        return this;
    }

    public TrafficEvent laneRuleId(String laneRuleId) {
        this.laneRuleId = laneRuleId;
        return this;
    }

    public TrafficEvent targetLane(String targetLane) {
        this.targetLane = targetLane;
        return this;
    }

    public TrafficEvent service(String service) {
        this.service = service;
        return this;
    }

    public TrafficEvent group(String group) {
        this.group = group;
        return this;
    }

    public TrafficEvent path(String path) {
        this.path = path;
        return this;
    }

    public TrafficEvent method(String method) {
        this.method = method;
        return this;
    }

    public TrafficEvent policyId(Long policyId) {
        this.policyId = policyId;
        return this;
    }

    public TrafficEvent policyTags(Map<String, String> policyTags) {
        this.policyTags = policyTags;
        return this;
    }

    public TrafficEvent actionType(ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    public TrafficEvent rejectType(RejectType rejectType) {
        this.rejectType = rejectType;
        return this;
    }

    public TrafficEvent requests(int requests) {
        this.requests = requests;
        return this;
    }

    public String getRejectTypeName() {
        return rejectType == null ? null : rejectType.name();
    }

    public static TrafficEvent build() {
        return new TrafficEvent();
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

}
