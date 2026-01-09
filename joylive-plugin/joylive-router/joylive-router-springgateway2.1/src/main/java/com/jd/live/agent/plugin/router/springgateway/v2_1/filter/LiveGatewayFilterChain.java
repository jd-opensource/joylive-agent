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
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * An extension of the GatewayFilterChain interface that allows for dynamic indexing of filters.
 */
public interface LiveGatewayFilterChain extends GatewayFilterChain {

    /**
     * Gets the current index of this filter in the chain.
     *
     * @return the index of this filter
     */
    int getIndex();

    /**
     * Sets the index of this filter in the chain.
     *
     * @param index the new index for this filter
     */
    void setIndex(int index);

    /**
     * Checks if the current route is configured to use a load balancer.
     *
     * @return true if the route is configured to use a load balancer, false otherwise
     */
    boolean isLoadbalancer();

    /**
     * A live gateway filter chain.
     */
    class DefaultGatewayFilterChain implements LiveGatewayFilterChain {

        private final List<GatewayFilter> filters;

        @Getter
        private final boolean loadbalancer;

        @Getter
        private int index;

        public DefaultGatewayFilterChain(List<GatewayFilter> filters) {
            this(filters, false);
        }

        public DefaultGatewayFilterChain(List<GatewayFilter> filters, boolean loadbalancer) {
            this.filters = filters;
            this.loadbalancer = loadbalancer;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            final int idx = index++;
            if (idx >= filters.size()) {
                return Mono.empty();
            }
            GatewayFilter filter = filters.get(idx);
            return filter.filter(exchange, this);
        }

        @Override
        public void setIndex(int index) {
            if (index >= 0) {
                this.index = index;
            }
        }

        public static GatewayFilterChain of(List<GatewayFilter> filters) {
            return new DefaultGatewayFilterChain(filters);
        }
    }
}