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
package com.jd.live.agent.plugin.router.springgateway.v2_1.filter;

import lombok.Getter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.function.BiFunction;

public class LiveRoute {

    @Getter
    private final Route route;

    @Getter
    private final RouteDefinition definition;

    @Getter
    private final long version;

    private volatile Object reference;

    private final Object mutex = new Object();

    public LiveRoute(Route route, RouteDefinition definition, long version) {
        this.route = route;
        this.definition = definition;
        this.version = version;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreate(BiFunction<Route, Long, T> function) {
        if (reference == null) {
            synchronized (mutex) {
                if (reference == null) {
                    reference = function.apply(route, version);
                }
            }
        }
        return (T) reference;
    }

}
