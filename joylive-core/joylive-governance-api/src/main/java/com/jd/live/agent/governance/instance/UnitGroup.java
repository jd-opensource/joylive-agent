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

import java.util.*;

/**
 * The UnitGroup class represents a collection of Endpoint objects that are grouped by a specific unit.
 * It also allows for further grouping of these endpoints into CellGroups based on the cell identifier
 * associated with each endpoint.
 */
public class UnitGroup {

    /**
     * The unit string that this UnitGroup represents. This is a final field that is initialized through
     * the constructor and should not be modified.
     */
    @Getter
    private final String unit;

    /**
     * A list of Endpoint objects that are part of this unit group. This list is managed by the add method
     * and should not be modified directly.
     */
    @Getter
    private final List<Endpoint> endpoints = new ArrayList<>(4);

    /**
     * A map that associates cell strings with CellGroup objects. Each CellGroup contains a collection
     * of Endpoint objects that share the same cell value within this unit.
     */
    @Getter
    private final Map<String, CellGroup> cellGroups;

    private CellGroup lastCellGroup;

    /**
     * Constructs a new UnitGroup with the specified unit and an empty list of endpoints.
     *
     * @param unit the unit string that this UnitGroup represents
     */
    public UnitGroup(String unit) {
        this.unit = Objects.requireNonNull(unit, "Unit cannot be null");
        this.cellGroups = new HashMap<>(5);
    }

    /**
     * Constructs a new UnitGroup with the specified unit and a list of endpoints.
     *
     * @param unit     the unit string that this UnitGroup represents
     * @param endpoints the initial list of endpoints to add to this unit group
     */
    public UnitGroup(String unit, List<? extends Endpoint> endpoints) {
        this(unit);
        if (endpoints != null) {
            endpoints.forEach(this::add);
        }
    }

    /**
     * Adds an Endpoint to this unit group and the corresponding cell group.
     *
     * @param endpoint the endpoint to be added
     */
    public void add(Endpoint endpoint) {
        if (endpoint != null) {
            endpoints.add(endpoint);
            String cell = endpoint.getCell();
            if (lastCellGroup == null || !lastCellGroup.getCell().equals(cell)) {
                lastCellGroup = cellGroups.computeIfAbsent(cell, c -> new CellGroup(unit, c));
            }
            lastCellGroup.add(endpoint);
        }
    }

    /**
     * Returns the CellGroup associated with the specified cell within this unit.
     *
     * @param cell the cell string to find the CellGroup for
     * @return the CellGroup for the specified cell, or null if not found
     */
    public CellGroup getCell(String cell) {
        return cell == null ? null : cellGroups.get(cell);
    }

    /**
     * Returns the total number of cell groups within this unit group.
     *
     * @return the size of the cellGroups map
     */
    public int getCells() {
        return cellGroups.size();
    }

    /**
     * Returns the total number of endpoints in this unit group.
     *
     * @return the size of the endpoints list
     */
    public int size() {
        return endpoints.size();
    }

    /**
     * Checks if this unit group is empty, meaning it contains no endpoints.
     *
     * @return true if the endpoints list is empty, false otherwise
     */
    public boolean isEmpty() {
        return endpoints.isEmpty();
    }

    /**
     * Returns the size of the cell group associated with the specified cell within this unit.
     *
     * @param cell the cell string to get the size of the associated CellGroup
     * @return the size of the CellGroup, or null if the cell or CellGroup is not found
     */
    public Integer getSize(String cell) {
        if (cell == null) {
            return null;
        } else {
            CellGroup group = cellGroups.get(cell);
            return group == null ? null : group.size();
        }
    }
}

