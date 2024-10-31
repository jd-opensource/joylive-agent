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

import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.instance.EndpointGroup;
import com.jd.live.agent.governance.instance.UnitGroup;
import com.jd.live.agent.governance.policy.live.Cell;
import com.jd.live.agent.governance.policy.live.CellRoute;
import com.jd.live.agent.governance.policy.live.Unit;
import com.jd.live.agent.governance.policy.live.UnitRoute;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The RouteTarget class is responsible for managing route targets, which may be
 * used in a workflow or task scheduling system. It provides functionalities to
 * handle a list of endpoints, perform actions on units, and manage routing logic.
 */
public class RouteTarget {

    /**
     * A list of instances that this route target is associated with.
     */
    private final List<? extends Endpoint> instances;

    /**
     * The group of endpoints that this route target is associated with.
     */
    @Getter
    private final EndpointGroup instanceGroup;

    /**
     * The unit group associated with the unit in this route target.
     */
    private UnitGroup unitGroup;

    /**
     * The unit that is the subject of the action in this route target.
     */
    @Getter
    private final Unit unit;

    /**
     * The action to be performed on the unit.
     */
    @Getter
    private final UnitAction unitAction;

    /**
     * The route associated with the unit.
     */
    @Getter
    private final UnitRoute unitRoute;

    /**
     * The cell route associated with this route target.
     */
    @Getter
    @Setter
    private CellRoute cellRoute;

    /**
     * A list of endpoints that this route target is currently operating on.
     */
    @Getter
    @Setter
    private List<? extends Endpoint> endpoints;

    /**
     * Constructs a new RouteTarget with the given parameters.
     *
     * @param instances   The list of endpoints associated with this route target.
     * @param instanceGroup The group of endpoints.
     * @param unit        The unit associated with this route target.
     * @param unitAction  The action to be performed on the unit.
     * @param unitRoute   The route associated with the unit.
     * @param cellRoute   The cell route associated with this route target.
     */
    public RouteTarget(List<? extends Endpoint> instances, EndpointGroup instanceGroup,
                       Unit unit, UnitAction unitAction, UnitRoute unitRoute, CellRoute cellRoute) {
        this.instances = instances == null && instanceGroup != null ? instanceGroup.getEndpoints() : instances;
        this.unit = unit == null && unitRoute != null ? unitRoute.getUnit() : unit;
        this.instanceGroup = instanceGroup == null && instances != null ? new EndpointGroup(instances) : instanceGroup;
        this.unitGroup = this.instanceGroup == null || this.unit == null ? null : this.instanceGroup.getUnitGroup(this.unit.getCode());
        this.unitAction = unitAction;
        this.unitRoute = unitRoute;
        this.cellRoute = cellRoute;
        this.endpoints = unitGroup != null ? unitGroup.getEndpoints() : this.instances;
    }

    public UnitGroup getUnitGroup() {
        // previous filters may filtrate the endpoints
        if (unitGroup != null && unitGroup.size() == size()) {
            return unitGroup;
        } else {
            unitGroup = new UnitGroup(unit.getCode(), endpoints);
            return unitGroup;
        }
    }

    /**
     * Gets the cell associated with this route target's cell route.
     *
     * @return The cell, or null if there is no cell route.
     */
    public Cell getCell() {
        return cellRoute == null ? null : cellRoute.getCell();
    }

    /**
     * Checks if the list of endpoints is empty.
     *
     * @return true if the list is null or empty, false otherwise.
     */
    public boolean isEmpty() {
        return endpoints == null || endpoints.isEmpty();
    }

    /**
     * Gets the size of the list of endpoints.
     *
     * @return The size of the list.
     */
    public int size() {
        return endpoints == null ? 0 : endpoints.size();
    }

    /**
     * Chooses a new list of endpoints based on the provided function.
     *
     * @param func The function to apply to the list of endpoints.
     */
    public void choose(Function<List<? extends Endpoint>, List<? extends Endpoint>> func) {
        if (func != null) {
            List<? extends Endpoint> values = func.apply(endpoints);
            endpoints = values != null ? values : new ArrayList<>();
        }
    }

    /**
     * Filters the list of endpoints based on the provided predicate.
     *
     * @param predicate The predicate to use for filtering.
     * @return The filtered list of endpoints.
     */
    public List<? extends Endpoint> filtrate(Predicate<Endpoint> predicate) {
        filter(endpoints, predicate, -1, true);
        return endpoints;
    }

    /**
     * Filters the list of endpoints based on the provided predicate and maximum size.
     *
     * @param predicate The predicate to use for filtering.
     * @param maxSize   The maximum size of the list to return.
     * @return The filtered list of endpoints.
     */
    public List<? extends Endpoint> filtrate(Predicate<Endpoint> predicate, int maxSize) {
        filter(endpoints, predicate, maxSize, true);
        return endpoints;
    }

    /**
     * Filters the list of endpoints based on the provided predicate and maximum size.
     *
     * @param predicate The predicate to use for filtering.
     * @param maxSize   The maximum size of the list to return.
     * @param nullable  Whether a null list is acceptable.
     * @return The filtered list of endpoints.
     */
    public List<? extends Endpoint> filtrate(Predicate<Endpoint> predicate, int maxSize, boolean nullable) {
        filter(endpoints, predicate, maxSize, nullable);
        return endpoints;
    }

    /**
     * Filters the list of endpoints based on the provided predicate.
     *
     * @param predicate The predicate to use for filtering.
     * @return The count of endpoints that matched the predicate.
     */
    public int filter(Predicate<Endpoint> predicate) {
        return filter(endpoints, predicate, -1, true);
    }

    /**
     * Filters the list of endpoints based on the provided predicate and maximum size.
     *
     * @param predicate The predicate to use for filtering.
     * @param maxSize   The maximum size of the list to return.
     * @return The count of endpoints that matched the predicate.
     */
    public int filter(Predicate<Endpoint> predicate, int maxSize) {
        return filter(endpoints, predicate, maxSize, true);
    }

    /**
     * Filters the list of endpoints based on the provided predicate and maximum size.
     *
     * @param predicate The predicate to use for filtering.
     * @param maxSize   The maximum size of the list to return.
     * @param nullable  Whether a null list is acceptable.
     * @return The count of endpoints that matched the predicate.
     */
    public int filter(Predicate<Endpoint> predicate, int maxSize, boolean nullable) {
        return filter(endpoints, predicate, maxSize, nullable);
    }

    /**
     * Creates a new list containing elements from the original list that match the given predicate.
     *
     * @param predicate The predicate to use for filtering. If null, the method returns the original list.
     * @return A new list containing the filtered endpoints. If the input list or predicate is null, returns the original list.
     */
    public List<? extends Endpoint> tryCopy(Predicate<Endpoint> predicate) {
        return tryCopy(endpoints, predicate, 0);
    }

    /**
     * Creates a new list containing elements from the original list that match the given predicate,
     * up to a specified maximum size.
     *
     * @param endpoints The list of endpoints to filter. If null or empty, the method returns the original list.
     * @param predicate The predicate to use for filtering. If null, the method returns the original list.
     * @param maxSize   The maximum size of the list to return. If non-positive, a default value is used.
     * @return A new list containing the filtered endpoints. If the input list or predicate is null, returns the original list.
     */
    public static List<? extends Endpoint> tryCopy(List<? extends Endpoint> endpoints, Predicate<Endpoint> predicate, int maxSize) {
        if (endpoints == null || endpoints.isEmpty() || (predicate == null && maxSize <= 0)) {
            return endpoints;
        }
        int count = 0;
        List<Endpoint> targets = null;
        for (Endpoint endpoint : endpoints) {
            if (predicate == null || predicate.test(endpoint)) {
                ++count;
                if (targets == null) {
                    targets = new ArrayList<>(Math.max(maxSize > 0 ? maxSize : endpoints.size() / 2, 1));
                }
                targets.add(endpoint);
                if (maxSize > 0 && count == maxSize) {
                    break;
                }
            }
        }
        return targets;
    }

    /**
     * Static method to filter a list of endpoints based on a predicate and an optional maximum size.
     *
     * @param endpoints The list of endpoints to filter.
     * @param predicate The predicate to use for filtering.
     * @param maxSize   The maximum size of the list to return.
     * @param nullable  Whether a null list is acceptable.
     * @param <T>       The type of the endpoints in the list.
     * @return The count of endpoints that matched the predicate.
     */
    public static <T extends Endpoint> int filter(List<T> endpoints, Predicate<Endpoint> predicate, int maxSize, boolean nullable) {
        int size = endpoints == null ? 0 : endpoints.size();
        if (size == 0 || (predicate == null && maxSize <= 0)) {
            return size;
        }

        int writeIndex = 0;
        // Traverse the list with readIndex to improve performance.
        for (int readIndex = 0; readIndex < size; readIndex++) {
            T endpoint = endpoints.get(readIndex);
            if (predicate == null || predicate.test(endpoint)) {
                if (writeIndex < readIndex) {
                    endpoints.set(writeIndex, endpoint);
                }
                writeIndex++;
                if (maxSize > 0 && writeIndex >= maxSize) {
                    break;
                }
            }
        }

        // Remove the remaining elements if any
        if ((writeIndex > 0 || nullable) && writeIndex < size) {
            if (writeIndex == 0) {
                endpoints.clear();
            } else {
                endpoints.subList(writeIndex, size).clear();
            }
        }
        return writeIndex;
    }

    /**
     * Static method to filter a list of endpoints based on a predicate.
     *
     * @param endpoints The list of endpoints to filter.
     * @param predicate The predicate to use for filtering.
     *                  @return The count of endpoints that matched the predicate.
     */
    public static int filter(List<? extends Endpoint> endpoints, Predicate<Endpoint> predicate) {
        return filter(endpoints, predicate, 0, false);
    }

    /**
     * Static method to create a RouteTarget instance that represents a rejection with a message.
     *
     * @param message The message for the rejection.
     * @return A new RouteTarget instance representing the rejection.
     */
    public static RouteTarget reject(String message) {
        return new RouteTarget(null, null, null, UnitAction.reject(message), null, null);
    }

    /**
     * Static method to create a RouteTarget instance that represents a rejection with a message and a unit.
     *
     * @param unit    The unit associated with the rejection.
     * @param message The message for the rejection.
     * @return A new RouteTarget instance representing the rejection.
     */
    public static RouteTarget reject(Unit unit, String message) {
        return new RouteTarget(null, null, unit, UnitAction.reject(message), null, null);
    }

    /**
     * Static method to create a RouteTarget instance that represents a forward action.
     *
     * @param instances The list of endpoints to forward to.
     * @return A new RouteTarget instance representing the forward action.
     */
    public static RouteTarget forward(List<? extends Endpoint> instances) {
        return new RouteTarget(instances, null, null, UnitAction.forward(), null, null);
    }

    /**
     * Static method to create a RouteTarget instance that represents a forward action with a unit route.
     *
     * @param instances The list of endpoints to forward to.
     * @param route     The unit route to forward to.
     * @return A new RouteTarget instance representing the forward action.
     */
    public static RouteTarget forward(List<? extends Endpoint> instances, UnitRoute route) {
        return new RouteTarget(instances, new EndpointGroup(instances), route.getUnit(), UnitAction.forward(), route, null);
    }

    /**
     * Static method to create a RouteTarget instance that represents a forward action with a group and a unit.
     *
     * @param group    The endpoint group to forward to.
     * @param unit     The unit to forward to.
     * @param route     The unit route to forward to.
     * @return A new RouteTarget instance representing the forward action.
     */
    public static RouteTarget forward(EndpointGroup group, Unit unit, UnitRoute route) {
        return new RouteTarget(null, group, unit, UnitAction.forward(), route, null);
    }

}
