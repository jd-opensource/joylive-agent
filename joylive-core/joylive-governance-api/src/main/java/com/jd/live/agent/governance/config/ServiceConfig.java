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

import com.jd.live.agent.core.util.trie.Path.PrefixPath;
import com.jd.live.agent.core.util.trie.PathMatchType;
import com.jd.live.agent.core.util.trie.PathMatcherTrie;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ServiceConfig is a configuration class that defines various settings for service behavior,
 * including failover thresholds and warmup configurations.
 */
public class ServiceConfig {

    /**
     * The name used to identify the service configuration component.
     */
    public static final String COMPONENT_SERVICE_CONFIG = "serviceConfig";

    /**
     * A flag to determine if the service should prioritize local resources first.
     */
    @Getter
    @Setter
    private boolean localFirst = true;

    /**
     * The local-first routing mode for this component.
     *
     * @see LocalFirstMode
     */
    @Getter
    @Setter
    private LocalFirstMode localFirstMode = LocalFirstMode.CELL;

    /**
     * A map of unit failover thresholds, where the key is the unit identifier and the value is the threshold integer.
     */
    @Getter
    @Setter
    private Map<String, Integer> unitFailoverThresholds;

    /**
     * A map of cell failover thresholds, where the key is the cell identifier and the value is the threshold integer.
     */
    @Getter
    @Setter
    private Map<String, Integer> cellFailoverThresholds;

    /**
     * A set of warmup identifiers for initialization or pre-warming up processes.
     */
    @Getter
    @Setter
    private Set<String> warmups;

    /**
     * The config of circuit breaker
     */
    @Getter
    @Setter
    private CircuitBreakerConfig circuitBreaker;

    /**
     * The config of concurrency limiter
     */
    @Getter
    @Setter
    private ConcurrencyLimiterConfig concurrencyLimiter;

    /**
     * The config of rate limiter
     */
    @Getter
    @Setter
    private RateLimiterConfig rateLimiter;

    @Getter
    @Setter
    private LoadLimiterConfig loadLimiter;

    /**
     * The config of system http inbound paths
     */
    @Getter
    @Setter
    private Set<String> systemPaths;

    /**
     * Define the grouping matching relationship for calls between services.
     * <p>k:v = service:group</p>
     */
    @Getter
    @Setter
    private Map<String, String> serviceGroups;

    /**
     * If the target service is not configured with a group, should all groups be allowed to call it
     */
    @Getter
    @Setter
    private boolean serviceGroupOpen = true;

    private final PathMatcherTrie<PrefixPath> systemPathTrie = new PathMatcherTrie<>(() -> {
        List<PrefixPath> result = new ArrayList<>();
        if (systemPaths != null) {
            systemPaths.forEach(path -> result.add(new PrefixPath(path)));
        }
        return result;
    });

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

    /**
     * Determines if the given path is system path.
     *
     * @param path the path to check.
     * @return {@code true} if the path is system path; {@code false} otherwise.
     */
    public boolean isSystem(String path) {
        return path != null && systemPaths != null && !systemPaths.isEmpty()
                && systemPathTrie.match(path, PathMatchType.PREFIX) != null;
    }
}

