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

import com.jd.live.agent.core.Constants;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The EndpointGroup class represents a collection of Endpoint objects that are grouped
 * based on their associated unit. This class provides methods to manage the collection
 * of endpoints and to access the grouping of endpoints by their unit.
 */
public class EndpointGroup {

    /**
     * A list of Endpoint objects that are part of this group. This list is initialized
     * through the constructor and should not be modified directly. Instead, use the
     * provided methods to interact with the endpoints.
     */
    @Getter
    private final List<Endpoint> endpoints;

    /**
     * A map that associates unit strings with UnitGroup objects. Each UnitGroup contains
     * a collection of Endpoint objects that share the same unit value.
     */
    @Getter
    private final Map<String, UnitGroup> unitGroups;

    /**
     * Constructs a new EndpointGroup with the specified list of endpoints. The endpoints
     * are automatically grouped by their unit into UnitGroup objects.
     *
     * @param endpoints the initial list of endpoints to group by unit; a null or empty list
     *                   results in an empty EndpointGroup
     */
    @SuppressWarnings("unchecked")
    public EndpointGroup(List<? extends Endpoint> endpoints) {
        this.endpoints = endpoints == null || endpoints.isEmpty() ? new ArrayList<>() : (List<Endpoint>) endpoints;
        this.unitGroups = new HashMap<>(3);
        UnitGroup last = null;
        String unit;
        for (Endpoint endpoint : this.endpoints) {
            unit = endpoint.getUnit();
            unit = (unit == null) ? Constants.DEFAULT_VALUE : unit;
            if (last == null || !last.getUnit().equals(unit)) {
                last = unitGroups.computeIfAbsent(unit, UnitGroup::new);
            }
            last.add(endpoint);
        }
    }

    /**
     * Returns the UnitGroup associated with the specified unit. If the unit is null or no endpoints
     * are associated with the unit, this method returns null.
     *
     * @param unit the unit string to find the UnitGroup for
     * @return the UnitGroup for the specified unit, or null if not found
     */
    public UnitGroup getUnitGroup(String unit) {
        return (unit == null) ? null : unitGroups.get(unit);
    }

    /**
     * Returns the total number of endpoints in this EndpointGroup.
     *
     * @return the size of the endpoints list
     */
    public int size() {
        return endpoints.size();
    }

    /**
     * Checks if this EndpointGroup is empty, meaning it contains no endpoints.
     *
     * @return true if the endpoints list is empty, false otherwise
     */
    public boolean isEmpty() {
        return endpoints.isEmpty();
    }
}

