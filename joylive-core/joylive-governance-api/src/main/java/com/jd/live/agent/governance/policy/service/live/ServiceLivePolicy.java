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
package com.jd.live.agent.governance.policy.service.live;

import com.jd.live.agent.core.parser.json.DeserializeConverter;
import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithId;
import com.jd.live.agent.governance.policy.service.annotation.Provider;
import com.jd.live.agent.governance.policy.service.live.converter.CellPolicyDeserializer;
import com.jd.live.agent.governance.policy.service.live.converter.UnitPolicyDeserializer;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service live policy
 */
@Provider
public class ServiceLivePolicy implements LiveStrategy, Cloneable, PolicyInheritWithId<ServiceLivePolicy> {

    /**
     * Unique identifier for the policy
     */
    @Getter
    @Setter
    private Long id;

    /**
     * Flag indicating if write operations are protected
     */
    @Getter
    @Setter
    @JsonAlias("writeProtected")
    private Boolean writeProtect;

    /**
     * A set of method names that should be retried in case of failure.
     */
    @Getter
    @Setter
    private Set<String> methods;

    /**
     * A set of method name prefixes that should be retried in case of failure.
     */
    @Getter
    @Setter
    private Set<String> methodPrefixes;

    /**
     * Expression for parsing unit variable
     */
    @Getter
    @Setter
    private String variableExpression;

    /**
     * Policy for unit-level operations
     */
    @Getter
    @Setter
    @DeserializeConverter(UnitPolicyDeserializer.class)
    private UnitPolicy unitPolicy;

    /**
     * Default unit failover threshold
     */
    @Getter
    @Setter
    private Integer defaultUnitThreshold;

    /**
     * Unit failover conditions
     */
    @Getter
    @Setter
    private List<RemoteCnd> unitRemotes;

    /**
     * Policy for cell-level operations
     */
    @Getter
    @Setter
    @DeserializeConverter(CellPolicyDeserializer.class)
    private CellPolicy cellPolicy;

    /**
     * Default cell failover threshold
     */
    @Getter
    @Setter
    private Integer defaultCellThreshold;

    /**
     * Cell failover conditions
     */
    @Getter
    @Setter
    private List<RemoteCnd> cellRemotes;

    private transient final Cache<String, RemoteCnd> unitRemoteCache = new MapCache<>(new ListBuilder<>(() -> unitRemotes, RemoteCnd::getName));

    private transient final Cache<String, RemoteCnd> cellRemoteCache = new MapCache<>(new ListBuilder<>(() -> cellRemotes, RemoteCnd::getName));

    /**
     * Retrieves the threshold for a specific unit based on its name.
     * If the unit is not found, returns the default unit threshold.
     *
     * @param name The name of the unit.
     * @return The threshold for the specified unit or the default threshold if not found.
     */
    public Integer getUnitThreshold(String name) {
        RemoteCnd cnd = unitRemoteCache.get(name);
        if (cnd == null) {
            return defaultUnitThreshold;
        }
        return cnd.getThreshold();
    }

    /**
     * Retrieves the threshold for a specific cell based on its name.
     * If the cell is not found, returns the default cell threshold.
     *
     * @param name The name of the cell.
     * @return The threshold for the specified cell or the default threshold if not found.
     */
    public Integer getCellThreshold(String name) {
        RemoteCnd cnd = cellRemoteCache.get(name);
        if (cnd == null) {
            return defaultCellThreshold;
        }
        return cnd.getThreshold();
    }

    public void cache() {
        unitRemoteCache.get("");
        cellRemoteCache.get("");
    }

    @Override
    public void supplement(ServiceLivePolicy source) {
        if (source == null) {
            return;
        }
        if (variableExpression == null) {
            variableExpression = source.getVariableExpression();
        }
        if (writeProtect == null) {
            writeProtect = source.getWriteProtect();
        }
        if ((methods == null || methods.isEmpty()) && source.methods != null) {
            methods = source.methods;
        }
        if ((methodPrefixes == null || methodPrefixes.isEmpty()) && source.methodPrefixes != null) {
            methodPrefixes = source.methodPrefixes;
        }
        if (unitPolicy == null) {
            unitPolicy = source.getUnitPolicy();
            defaultUnitThreshold = source.getDefaultUnitThreshold();
        }
        if (defaultUnitThreshold == null) {
            defaultUnitThreshold = source.getDefaultUnitThreshold();
        }
        if ((unitRemotes == null || unitRemotes.isEmpty()) && (source.getUnitRemotes() != null && !source.getUnitRemotes().isEmpty())) {
            unitRemotes = unitRemotes == null ? new ArrayList<>() : unitRemotes;
            source.getUnitRemotes().forEach(o -> unitRemotes.add(o.clone()));
        }
        if (cellPolicy == null) {
            cellPolicy = source.cellPolicy;
            defaultCellThreshold = source.getDefaultCellThreshold();
        }
        if (defaultCellThreshold == null) {
            defaultCellThreshold = source.getDefaultCellThreshold();
        }
        if ((cellRemotes == null || cellRemotes.isEmpty()) && (source.getCellRemotes() != null && !source.getCellRemotes().isEmpty())) {
            cellRemotes = cellRemotes == null ? new ArrayList<>() : cellRemotes;
            source.getCellRemotes().forEach(o -> cellRemotes.add(o.clone()));
        }
    }

    /**
     * Checks whether the specified method is write-protected.
     *
     * @param methodName The name of the method to check.
     * @return <code>true</code> if the method is write-protected, otherwise <code>false</code>.
     */
    public boolean isWriteProtect(String methodName) {
        if (writeProtect == null || !writeProtect || methodName == null || methodName.isEmpty()) {
            return false;
        }
        boolean allowList = false;
        if (methods != null && !methods.isEmpty()) {
            allowList = true;
            if (methods.contains(methodName)) {
                return true;
            }
        }
        if (methodPrefixes != null && !methodPrefixes.isEmpty()) {
            allowList = true;
            for (String methodPrefix : methodPrefixes) {
                if (methodName.startsWith(methodPrefix)) {
                    return true;
                }
            }
        }
        return !allowList;
    }

    public ServiceLivePolicy clone() {
        try {
            return (ServiceLivePolicy) super.clone();
        } catch (CloneNotSupportedException ignore) {
            return null;
        }
    }

}
