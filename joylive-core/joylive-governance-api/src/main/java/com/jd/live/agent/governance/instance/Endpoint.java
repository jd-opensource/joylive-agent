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
package com.jd.live.agent.governance.instance;

import com.jd.live.agent.core.util.matcher.Matcher;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.rule.tag.TagCondition;

import java.util.List;

/**
 * Represents an endpoint in a distributed system, providing methods to access its properties and match against tag conditions.
 * Endpoints are fundamental entities that can represent services, nodes, or instances within a system.
 */
public interface Endpoint extends Matcher<TagCondition> {

    /**
     * Key for the timestamp property of the endpoint.
     */
    String KEY_TIMESTAMP = "timestamp";

    /**
     * Key for the weight property of the endpoint.
     */
    String KEY_WEIGHT = "weight";

    /**
     * Key for the warmup period property of the endpoint.
     */
    String KEY_WARMUP = "warmup";

    /**
     * Default warmup period for the endpoint in milliseconds.
     */
    int DEFAULT_WARMUP = 10 * 60 * 1000;

    /**
     * Default weight for the endpoint.
     */
    int DEFAULT_WEIGHT = 100;

    /**
     * Gets the unique identifier of the endpoint.
     * By default, this is the address of the endpoint.
     *
     * @return The identifier of the endpoint.
     */
    default String getId() {
        return getAddress();
    }

    /**
     * Gets the address of the endpoint, typically in the format "host:port".
     * If the port is not specified or invalid, only the host is returned.
     *
     * @return The address of the endpoint.
     */
    default String getAddress() {
        String host = getHost();
        int port = getPort();
        return port <= 0 ? host : host + ":" + port;
    }

    /**
     * Gets the host of the endpoint.
     *
     * @return The host of the endpoint.
     */
    String getHost();

    /**
     * Gets the port of the endpoint.
     *
     * @return The port of the endpoint, or a non-positive value if not applicable.
     */
    int getPort();

    /**
     * Gets the timestamp associated with the endpoint.
     * This can be used for various purposes, such as versioning or timing.
     *
     * @return The timestamp, or null if not available.
     */
    default Long getTimestamp() {
        return null;
    }

    /**
     * Gets the weight of the endpoint for load balancing purposes.
     * The weight can influence the distribution of requests among multiple endpoints.
     *
     * @param request The service request for which the weight is being determined.
     * @return The weight of the endpoint, or null if not specified.
     */
    default Integer getWeight(ServiceRequest request) {
        return null;
    }

    /**
     * Gets the live space ID associated with this endpoint.
     *
     * @return The live space ID, or the default value if not specified.
     */
    default String getLiveSpaceId() {
        return getLabel(Label.LABEL_LIVESPACE_ID, Label.DEFAULT_VALUE);
    }

    /**
     * Gets the unit associated with this endpoint.
     *
     * @return The unit, or the default value if not specified.
     */
    default String getUnit() {
        return getLabel(Label.LABEL_UNIT, Label.DEFAULT_VALUE);
    }

    /**
     * Gets the cell associated with this endpoint.
     *
     * @return The cell, or the default value if not specified.
     */
    default String getCell() {
        return getLabel(Label.LABEL_CELL, Label.DEFAULT_VALUE);
    }

    /**
     * Gets the region associated with this endpoint.
     *
     * @return The region, or the default value if not specified.
     */
    default String getRegion() {
        return getLabel(Label.LABEL_REGION, Label.DEFAULT_VALUE);
    }

    /**
     * Gets the zone associated with this endpoint.
     *
     * @return The zone, or the default value if not specified.
     */
    default String getZone() {
        return getLabel(Label.LABEL_ZONE, Label.DEFAULT_VALUE);
    }

    /**
     * Gets the lane space ID associated with this endpoint.
     *
     * @return The lane space ID, or the default value if not specified.
     */
    default String getLaneSpaceId() {
        return getLabel(Label.LABEL_LANESPACE_ID, Label.DEFAULT_VALUE);
    }

    /**
     * Gets the lane associated with this endpoint.
     *
     * @return The lane, or the default value if not specified.
     */
    default String getLane() {
        return getLabel(Label.LABEL_LANE, Label.DEFAULT_VALUE);
    }

    /**
     * Determines if the live space ID matches the specified live space ID.
     *
     * @param liveSpaceId The live space ID to match.
     * @return true if the live space ID matches, false otherwise.
     */
    default boolean isLiveSpace(long liveSpaceId) {
        return liveSpaceId == Converts.getLong(getLiveSpaceId(), 0L);
    }

    /**
     * Determines if the unit matches the specified unit.
     *
     * @param unit The unit to match.
     * @return true if the unit matches, false otherwise.
     */
    default boolean isUnit(String unit) {
        return getUnit().equals(unit);
    }

    /**
     * Determines if the live space and unit match the specified live space ID and unit.
     *
     * @param liveSpaceId The live space ID to match.
     * @param unit        The unit to match.
     * @return true if both the live space ID and unit match, false otherwise.
     */
    default boolean isUnit(long liveSpaceId, String unit) {
        return isLiveSpace(liveSpaceId) && isUnit(unit);
    }

    /**
     * Determines if the cell matches the specified cell.
     *
     * @param cell The cell to match.
     * @return true if the cell matches, false otherwise.
     */
    default boolean isCell(String cell) {
        return getCell().equals(cell);
    }

    /**
     * Determines if the live space and cell match the specified live space ID and cell.
     *
     * @param liveSpaceId The live space ID to match.
     * @param cell        The cell to match.
     * @return true if both the live space ID and cell match, false otherwise.
     */
    default boolean isCell(long liveSpaceId, String cell) {
        return isLiveSpace(liveSpaceId) && isCell(cell);
    }

    /**
     * Determines if the lane space ID matches the specified lane space ID.
     *
     * @param laneSpaceId The lane space ID to match.
     * @return true if the lane space ID matches, false otherwise.
     */
    default boolean isLaneSpace(long laneSpaceId) {
        return laneSpaceId == Converts.getLong(getLaneSpaceId(), 0L);
    }

    /**
     * Determines if the lane matches the specified lane.
     *
     * @param lane The lane to match.
     * @return true if the lane matches, false otherwise.
     */
    default boolean isLane(String lane) {
        return getLane().equals(lane);
    }

    /**
     * Determines if the lane space and lane match the specified lane space ID and lane.
     *
     * @param laneSpaceId The lane space ID to match.
     * @param lane        The lane to match.
     * @return true if both the lane space ID and lane match, false otherwise.
     */
    default boolean isLane(long laneSpaceId, String lane) {
        return isLaneSpace(laneSpaceId) && isLane(lane);
    }

    /**
     * Gets a label's value based on the specified key.
     *
     * @param key The key of the label to retrieve.
     * @return The value of the label, or null if not found.
     */
    String getLabel(String key);

    /**
     * Gets a list of label values based on the specified key.
     *
     * @param key The key of the labels to retrieve.
     * @return A list of label values, or null if not found.
     */
    default List<String> getLabels(String key) {
        String value = getLabel(key);
        return value == null ? null : Label.parseValue(value);
    }

    /**
     * Gets a label's value based on the specified key, returning a default value if the label is not found.
     *
     * @param key          The key of the label to retrieve.
     * @param defaultValue The default value to return if the label is not found.
     * @return The value of the label, or the default value if not found.
     */
    default String getLabel(String key, String defaultValue) {
        String result = getLabel(key);
        return result == null ? defaultValue : result;
    }

    /**
     * Gets the state of the endpoint.
     * The state can indicate the current health or status of the endpoint, such as active, inactive, or in maintenance.
     *
     * @return The current state of the endpoint.
     */
    EndpointState getState();

    /**
     * Checks if the endpoint is accessible based on its current state.
     * <p>
     * This method returns {@code true} if the endpoint's state is either {@code null}
     * (which implies that no state information is available and the endpoint is assumed to be accessible),
     * or if the state is not {@code null} and the state indicates that the endpoint is accessible.
     *
     * @return {@code true} if the endpoint is accessible or if no state information is available,
     * {@code false} if the endpoint's state indicates that it is not accessible.
     */
    default boolean isAccessible() {
        EndpointState state = getState();
        return state == null || state.isAccessible();
    }

    /**
     * Evaluates the predicate associated with this endpoint, if any, to determine
     * if this endpoint satisfies the conditions defined by the predicate.
     * <p>
     * This method will return {@code true} if no predicate is set, implying that
     * the endpoint is acceptable by default. If a predicate is set, the endpoint
     * will be tested against it.
     * </p>
     *
     * @return {@code true} if the predicate is {@code null} or if the predicate
     *         test passes for this endpoint; {@code false} otherwise.
     */
    default boolean predicate() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean match(TagCondition source) {
        return source == null || source.match(getLabels(source.getKey()));
    }
}
