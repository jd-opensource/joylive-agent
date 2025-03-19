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
package com.jd.live.agent.plugin.router.springgateway.v2_2.filter;

import lombok.Getter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.Route;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class representing a live filter for a route.
 */
@Getter
public class LiveRouteFilter {

    /**
     * The global route version counter.
     */
    public static final AtomicLong ROUTE_VERSION = new AtomicLong(0);

    /**
     * The route associated with this live filter.
     */
    private final Route route;

    /**
     * The list of gateway filters for this live filter.
     */
    private final List<GatewayFilter> filters;

    /**
     * The list of path filters for this live filter.
     */
    private final List<GatewayFilter> pathFilters;

    /**
     * The version of this live filter.
     */
    private final long version;

    /**
     * Creates a new instance of LiveFilter.
     *
     * @param route       the route associated with this live filter
     * @param filters     the list of gateway filters for this live filter
     * @param pathFilters the list of path filters for this live filter
     * @param version     the version of this live filter
     */
    public LiveRouteFilter(Route route, List<GatewayFilter> filters, List<GatewayFilter> pathFilters, long version) {
        this.route = route;
        this.filters = filters;
        this.pathFilters = pathFilters;
        this.version = version;
    }
}
