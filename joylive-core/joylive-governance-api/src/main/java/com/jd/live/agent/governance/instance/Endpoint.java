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

import com.jd.live.agent.bootstrap.util.Attributes;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.matcher.Matcher;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.instance.counter.Counter;
import com.jd.live.agent.governance.instance.counter.EndpointCounter;
import com.jd.live.agent.governance.instance.counter.ServiceCounter;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.rule.tag.TagCondition;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.StringUtils.isEqualsOrEmpty;

/**
 * Represents an endpoint in a distributed system, providing methods to access its properties and match against tag conditions.
 * Endpoints are fundamental entities that can represent services, nodes, or instances within a system.
 */
public interface Endpoint extends Matcher<TagCondition>, Attributes {

    Predicate<String> SECURE_SCHEME = scheme -> "https".equals(scheme) || "wss".equals(scheme);

    String DEFAULT_HTTP_SCHEME = "http";

    /**
     * Key for the counter attribute of the endpoint.
     */
    String ATTRIBUTE_COUNTER = "counter";

    /**
     * Key for the counter attribute of the endpoint.
     */
    String ATTRIBUTE_URI = "uri";

    /**
     * Default warmup period for the endpoint in milliseconds.
     */
    int DEFAULT_WARMUP = 2 * 60 * 1000;

    /**
     * Default weight for the endpoint.
     */
    int DEFAULT_WEIGHT = 100;

    String STATE_HEALTHY = "healthy";

    String STATE_HANGUP = "hangup";

    String STATE_SUSPEND = "suspend";

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
        return URI.getAddress(getHost(), getPort());
    }

    default String getScheme() {
        return null;
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
        return Converts.getLong(getLabel(Constants.LABEL_TIMESTAMP), null);
    }

    /**
     * Gets the warm-up time for this endpoint.
     *
     * @return the warm-up time in seconds, or the default value if not specified
     */
    default Integer getWarmup() {
        return Converts.getInteger(getLabel(Constants.LABEL_WARMUP), DEFAULT_WARMUP);
    }

    /**
     * Retrieves the weight ratio associated with the circuit breaker.
     *
     * @return the weight ratio as a double value, or null if not set
     */
    default Double getWeightRatio() {
        return null;
    }

    /**
     * Sets the weight ratio for the circuit breaker.
     *
     * @param weightRatio the weight ratio to be set
     */
    default void setWeightRatio(Double weightRatio) {
    }

    /**
     * Gets the weight for the specified service request, taking into account the weight, warm-up time and recover time.
     *
     * @param request the service request for which to get the weight
     * @return the weight for this endpoint
     */
    default Integer reweight(ServiceRequest request) {
        int weight = getWeight(request);
        if (weight > 0) {
            long now = System.currentTimeMillis();
            Double ratio = getWeightRatio();
            weight = getWeight(weight, getTimestamp(), getWarmup(), now);
            weight = ratio != null ? (int) (weight * ratio) : weight;
            return weight < 0 ? 0 : Math.max(1, weight);
        }
        return 0;
    }

    /**
     * Calculates the effective weight of a resource based on its uptime and warmup period.
     * <p>
     * This method calculates the effective weight of a resource considering its initial weight,
     * the timestamp of its start, a warmup period, and the current time. The weight is adjusted
     * during the warmup period and returns the original weight once the warmup period is complete.
     *
     * @param weight    the initial weight of the resource
     * @param timestamp the timestamp when the resource started
     * @param duration  the time window in milliseconds
     * @param now       the current timestamp
     * @return the effective weight of the resource
     */
    static int getWeight(int weight, Long timestamp, Integer duration, long now) {
        if (weight <= 0 || timestamp == null || timestamp <= 0 || duration == null || duration <= 0) {
            return weight;
        }
        long span = now - timestamp;
        if (span <= 0) {
            return -1;
        } else if (span < duration) {
            return (int) (span / ((float) duration / weight));
        }
        return weight;
    }

    /**
     * Gets the origin weight for the specified service request.
     *
     * @param request the service request for which to get the origin weight
     * @return the origin weight, or the default value if not specified
     */
    default Integer getWeight(ServiceRequest request) {
        return Converts.getInteger(getLabel(Constants.LABEL_WEIGHT), DEFAULT_WEIGHT);
    }

    /**
     * Gets the live space ID associated with this endpoint.
     *
     * @return The live space ID, or the default value if not specified.
     */
    default String getLiveSpaceId() {
        return getLabel(Constants.LABEL_LIVE_SPACE_ID, Constants.DEFAULT_VALUE);
    }

    /**
     * Gets the unit associated with this endpoint.
     *
     * @return The unit, or the default value if not specified.
     */
    default String getUnit() {
        return getLabel(Constants.LABEL_UNIT, Constants.DEFAULT_VALUE);
    }

    /**
     * Gets the cell associated with this endpoint.
     *
     * @return The cell, or the default value if not specified.
     */
    default String getCell() {
        return getLabel(Constants.LABEL_CELL, Constants.DEFAULT_VALUE);
    }

    /**
     * Gets the cloud associated with this endpoint.
     *
     * @return The cloud, or the default value if not specified.
     */
    default String getCloud() {
        return getLabel(Constants.LABEL_CLOUD, Constants.DEFAULT_VALUE);
    }

    /**
     * Gets the region associated with this endpoint.
     *
     * @return The region, or the default value if not specified.
     */
    default String getRegion() {
        return getLabel(Constants.LABEL_REGION, Constants.DEFAULT_VALUE);
    }

    /**
     * Gets the zone associated with this endpoint.
     *
     * @return The zone, or the default value if not specified.
     */
    default String getZone() {
        return getLabel(Constants.LABEL_ZONE, Constants.DEFAULT_VALUE);
    }

    /**
     * Gets the lane space ID associated with this endpoint.
     *
     * @return The lane space ID, or the default value if not specified.
     */
    default String getLaneSpaceId() {
        return getLabel(Constants.LABEL_LANE_SPACE_ID, Constants.DEFAULT_VALUE);
    }

    /**
     * Gets the lane space ID associated with this endpoint.
     *
     * @return The lane space ID, or the default value if not specified.
     */
    default String getLaneSpaceId(String defaultValue) {
        return getLabel(Constants.LABEL_LANE_SPACE_ID, defaultValue);
    }

    /**
     * Gets the lane associated with this endpoint.
     *
     * @return The lane, or the default value if not specified.
     */
    default String getLane() {
        return getLabel(Constants.LABEL_LANE, Constants.DEFAULT_VALUE);
    }

    /**
     * Gets the lane associated with this endpoint.
     *
     * @param defaultValue The default value to return if the lane is not specified.
     * @return The lane, or the default value if not specified.
     */
    default String getLane(String defaultValue) {
        return getLabel(Constants.LABEL_LANE, defaultValue);
    }

    /**
     * Get the service group associated with this endpoint
     *
     * @return The lane, or the default value if not specified.
     */
    default String getGroup() {
        return getLabel(Constants.LABEL_SERVICE_GROUP, PolicyId.DEFAULT_GROUP);
    }

    /**
     * Checks if the live space ID is null or empty.
     *
     * @return true if the live space ID is null or empty, false otherwise
     */
    default boolean isLiveless() {
        String spaceId = getLiveSpaceId();
        return spaceId == null || spaceId.isEmpty();
    }

    /**
     * Determines if the live space ID matches the specified live space ID.
     *
     * @param liveSpaceId The live space ID to match.
     * @return true if the live space ID matches, false otherwise.
     */
    default boolean isLiveSpace(String liveSpaceId) {
        return liveSpaceId != null && liveSpaceId.equals(getLiveSpaceId());
    }

    /**
     * Determines if the unit matches the specified unit.
     *
     * @param unit The unit to match.
     * @return true if the unit matches, false otherwise.
     */
    default boolean isUnit(String unit) {
        return unit != null && unit.equals(getUnit());
    }

    /**
     * Determines if the unit matches the specified units.
     *
     * @param units The units to match.
     * @return true if the unit matches, false otherwise.
     */
    default boolean isUnit(Set<String> units) {
        if (units == null || units.isEmpty()) {
            return false;
        }
        String unit = getUnit();
        return unit != null && units.contains(unit);
    }

    /**
     * Determines if the live space and unit match the specified live space ID and unit.
     *
     * @param liveSpaceId The live space ID to match.
     * @param unit        The unit to match.
     * @return true if both the live space ID and unit match, false otherwise.
     */
    default boolean isUnit(String liveSpaceId, String unit) {
        return isLiveSpace(liveSpaceId) && isUnit(unit);
    }

    /**
     * Determines if the live space and unit match the specified live space ID and units.
     *
     * @param liveSpaceId The live space ID to match.
     * @param units       The units to match.
     * @return true if both the live space ID and unit match, false otherwise.
     */
    default boolean isUnit(String liveSpaceId, Set<String> units) {
        return isLiveSpace(liveSpaceId) && isUnit(units);
    }

    /**
     * Determines if the cell matches the specified cell.
     *
     * @param cell The cell to match.
     * @return true if the cell matches, false otherwise.
     */
    default boolean isCell(String cell) {
        return cell != null && cell.equals(getCell());
    }

    /**
     * Determines if the live space and cell match the specified live space ID and cell.
     *
     * @param liveSpaceId The live space ID to match.
     * @param cell        The cell to match.
     * @return true if both the live space ID and cell match, false otherwise.
     */
    default boolean isCell(String liveSpaceId, String cell) {
        return isLiveSpace(liveSpaceId) && isCell(cell);
    }

    /**
     * Determines if the lane space and lane match the specified lane space ID and lane.
     *
     * @param laneSpaceId    The lane space ID to match.
     * @param lane           The lane to match.
     * @param defaultSpaceId The default lane space ID.
     * @param defaultLane    The default lane.
     * @return true if both the lane space ID and lane match, false otherwise.
     */
    default boolean isLane(String laneSpaceId, String lane, String defaultSpaceId, String defaultLane) {
        return isEqualsOrEmpty(getLaneSpaceId(defaultSpaceId), laneSpaceId) && isEqualsOrEmpty(getLane(defaultLane), lane);
    }

    /**
     * Determines if the cloud label matches the specified cloud.
     *
     * @param cloud The cloud to match.
     * @return true if the unit matches, false otherwise.
     */
    default boolean isCloud(String cloud) {
        return cloud != null && !cloud.isEmpty() && cloud.equals(getLabel(Constants.LABEL_CLOUD));
    }

    /**
     * Determines if the cluster label matches the specified cluster.
     *
     * @param cluster The cluster to match.
     * @return true if the unit matches, false otherwise.
     */
    default boolean isCluster(String cluster) {
        return cluster != null && !cluster.isEmpty() && cluster.equals(getLabel(Constants.LABEL_CLUSTER));
    }

    /**
     * Determines if the group label matches the specified group.
     *
     * @param group The group to match.
     * @return true if the unit matches, false otherwise.
     */
    default boolean isGroup(String group) {
        String label = getGroup();
        // default group
        if (group == null || group.isEmpty() || PolicyId.DEFAULT_GROUP.equals(group)) {
            return label == null || label.isEmpty() || PolicyId.DEFAULT_GROUP.equalsIgnoreCase(label);
        } else {
            return group.equals(label);
        }
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
        return result == null || result.isEmpty() ? defaultValue : result;
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
     * Checks if the specified port matches the current port.
     *
     * @param port The port to check against the current port.
     * @return {@code true} if the specified port matches the current port, {@code false} otherwise.
     */
    default boolean isPort(int port) {
        return getPort() == port;
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

    /**
     * Retrieves a {@link Counter} instance associated with the specified {@link ServiceCounter}, URI, and access time.
     * This method ensures that a counter is always available for tracking metrics or statistics related to the specified endpoint.
     * If the {@link ServiceCounter} or URI is {@code null}, this method returns {@code null}.
     *
     * @param serviceCounter the {@link ServiceCounter} instance used to manage counters for services
     * @param uri            the URI of the service endpoint for which the counter is retrieved
     * @param time           the access time to set, typically in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT)
     * @return the {@link Counter} instance associated with the specified URI, or {@code null} if {@link ServiceCounter} or URI is {@code null}
     */
    default Counter getCounter(ServiceCounter serviceCounter, URI uri, long time) {
        if (serviceCounter == null || uri == null) {
            return null;
        }
        EndpointCounter endpointCounter = serviceCounter.getOrCreateCounter(getId());
        endpointCounter.setAccessTime(time);
        return endpointCounter.getOrCreateCounter(uri);
    }

}
