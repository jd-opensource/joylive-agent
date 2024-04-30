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

import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * The CellGroup class represents a collection of endpoints grouped by a specific cell and unit.
 * This class is used to organize endpoints in a way that allows for efficient management and access,
 * particularly in the context of cellular network management or similar scenarios.
 */
@Getter
public class CellGroup {

    /**
     * The unit associated with this cell group, which could represent a geographical area,
     * network segment, or other logical division.
     */
    private final String unit;

    /**
     * The identifier for the cell within the unit, which could be a cell tower ID, a subnet ID,
     * or any other identifier used to distinguish cells within the unit.
     */
    private final String cell;

    /**
     * A list of Endpoint objects that belong to this cell group. Endpoints are typically network
     * devices or services that are associated with the cell for routing or management purposes.
     */
    private final List<Endpoint> endpoints;

    /**
     * Constructs a new CellGroup with the specified unit and cell, and an empty list of endpoints.
     *
     * @param unit the unit associated with this cell group
     * @param cell the identifier for the cell within the unit
     */
    public CellGroup(String unit, String cell) {
        this.unit = Objects.requireNonNull(unit, "Unit cannot be null");
        this.cell = Objects.requireNonNull(cell, "Cell cannot be null");
        this.endpoints = new LinkedList<>();
    }

    /**
     * Adds an Endpoint to this cell group. If the provided endpoint is null, it is not added.
     *
     * @param endpoint the endpoint to be added to the cell group
     */
    public void add(Endpoint endpoint) {
        if (endpoint != null) {
            endpoints.add(endpoint);
        }
    }

    /**
     * Returns the number of endpoints in this cell group.
     *
     * @return the size of the endpoints list
     */
    public int size() {
        return endpoints.size();
    }

    /**
     * Checks if this cell group is empty, meaning it contains no endpoints.
     *
     * @return true if the endpoints list is empty, false otherwise
     */
    public boolean isEmpty() {
        return endpoints.isEmpty();
    }
}

