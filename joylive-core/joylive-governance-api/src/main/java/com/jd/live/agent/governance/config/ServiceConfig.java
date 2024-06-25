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
package com.jd.live.agent.governance.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/**
 * ServiceConfig is a configuration class that defines various settings for service behavior,
 * including failover thresholds and warmup configurations.
 */
@Getter
@Setter
public class ServiceConfig {

    /**
     * The name used to identify the service configuration component.
     */
    public static final String COMPONENT_SERVICE_CONFIG = "serviceConfig";

    /**
     * A flag to determine if the service should prioritize local resources first.
     */
    private boolean localFirst = false;

    /**
     * A map of unit failover thresholds, where the key is the unit identifier and the value is the threshold integer.
     */
    private Map<String, Integer> unitFailoverThresholds;

    /**
     * A map of cell failover thresholds, where the key is the cell identifier and the value is the threshold integer.
     */
    private Map<String, Integer> cellFailoverThresholds;

    /**
     * A set of warmup identifiers for initialization or pre-warming up processes.
     */
    private Set<String> warmups;

    /**
     * A set of exclude class full name.
     */
    private Set<String> excludes;

    /**
     * Retrieves the failover threshold for a given unit.
     *
     * @param unit the identifier of the unit to get the failover threshold for
     * @return the failover threshold for the unit, or null if the unit or thresholds map is null
     */
    public Integer getUnitFailoverThreshold(String unit) {
        return unit == null || unitFailoverThresholds == null ? null : unitFailoverThresholds.get(unit);
    }

    /**
     * Retrieves the failover threshold for a given cell.
     *
     * @param cell the identifier of the cell to get the failover threshold for
     * @return the failover threshold for the cell, or null if the cell or thresholds map is null
     */
    public Integer getCellFailoverThreshold(String cell) {
        return cell == null || cellFailoverThresholds == null ? null : cellFailoverThresholds.get(cell);
    }

    public boolean isExclude(String type) {
        return type != null && excludes != null && excludes.contains(type);
    }
}

