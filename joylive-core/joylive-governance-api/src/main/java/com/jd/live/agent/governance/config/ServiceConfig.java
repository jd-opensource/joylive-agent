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

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.trie.Path.PrefixPath;
import com.jd.live.agent.core.util.trie.PathMatchType;
import com.jd.live.agent.core.util.trie.PathMatcherTrie;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * ServiceConfig is a configuration class that defines various settings for service behavior,
 * including failover thresholds and warmup configurations.
 */
public class ServiceConfig {

    private static final String[] SPRING_SYSTEM_HANDLERS = new String[]{
            "org.springframework.web.servlet.mvc.Controller",
            "org.springframework.boot.web.servlet.error.ErrorController",
            "org.springframework.web.servlet.resource.ResourceHttpRequestHandler",
            "org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping$WebMvcEndpointHandlerMethod",
            "springfox.documentation.swagger.web.ApiResourceController",
            "springfox.documentation.swagger2.web.Swagger2ControllerWebMvc",
            "org.springdoc.webmvc.api.OpenApiResource",
            "org.springdoc.webmvc.api.MultipleOpenApiResource",
            "org.springdoc.webmvc.ui.SwaggerConfigResource",
            "org.springdoc.webmvc.ui.SwaggerUiHome",
            "org.springdoc.webmvc.ui.SwaggerWelcomeCommon",
            "org.springframework.boot.actuate.endpoint.web.reactive.AbstractWebFluxEndpointHandlerMapping$WebFluxEndpointHandlerMethod",
            "org.springframework.web.reactive.resource.ResourceWebHandler",
            "springfox.documentation.swagger2.web.Swagger2ControllerWebFlux"
    };

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

    @Getter
    @Setter
    private MonitorConfig monitor;

    /**
     * The config of system http inbound paths
     */
    @Getter
    @Setter
    private Set<String> systemPaths;

    @Getter
    @Setter
    private Set<String> systemHandlers;

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

    @Getter
    @Setter
    private boolean responseException = true;

    @Getter
    @Setter
    private String defaultResultType;

    @Getter
    private transient final LazyObject<Class<?>> defaultResultTypeCache = new LazyObject<>(null);

    private transient final PathMatcherTrie<PrefixPath> systemPathTrie = new PathMatcherTrie<>(() -> {
        List<PrefixPath> result = new ArrayList<>();
        if (systemPaths != null) {
            systemPaths.forEach(path -> result.add(new PrefixPath(path)));
        }
        return result;
    });

    private transient final Map<Class, Boolean> systemHandlerCache = new ConcurrentHashMap<>();

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
     * Returns the group associated with the specified service.
     *
     * @param service the name of the service
     * @return the group associated with the service, or null if the service is not found or if the service groups map is null
     */
    public String getGroup(String service) {
        return serviceGroups == null || service == null ? null : serviceGroups.get(service);
    }

    /**
     * Gets the generic result class, loading it if necessary.
     *
     * @param classLoader the class loader to use for loading the class
     * @return the generic result class, or null if not available
     */
    public Class<?> getDefaultResultClass(ClassLoader classLoader) {
        if (classLoader != null && !isEmpty(defaultResultType)) {
            // load class by application classloader.
            return defaultResultTypeCache.get(() -> loadClass(defaultResultType, classLoader, false));
        }
        return null;
    }

    /**
     * Determines if the given path is system path.
     *
     * @param path the path to check.
     * @return {@code true} if the path is system path; {@code false} otherwise.
     */
    public boolean isSystemPath(String path) {
        return path != null && systemPaths != null && !systemPaths.isEmpty()
                && systemPathTrie.match(path, PathMatchType.PREFIX) != null;
    }

    /**
     * Checks if the given type is a system handler class or inherits from one.
     *
     * @param type the class to check
     * @return true if the class is a system handler or inherits from one, false otherwise
     */
    public boolean isSystemHandler(Class<?> type) {
        if (type == null || systemHandlers == null || systemHandlers.isEmpty()) {
            return false;
        }
        return systemHandlerCache.computeIfAbsent(type, c -> {
            Queue<Class<?>> queue = new LinkedList<>();
            queue.add(c);
            Class<?> current;
            while (!queue.isEmpty()) {
                current = queue.poll();
                if (systemHandlers.contains(current.getName())) {
                    return true;
                }
                for (Class<?> intfType : current.getInterfaces()) {
                    queue.add(intfType);
                }
                Class<?> parent = current.getSuperclass();
                if (parent != null && parent != Object.class) {
                    queue.add(parent);
                }
            }
            return false;
        });
    }

    protected void initialize() {
        loadLimiter.initialize();
        if (systemHandlers == null) {
            systemHandlers = new HashSet<>();
        }
        systemHandlers.addAll(Arrays.asList(SPRING_SYSTEM_HANDLERS));
    }
}

