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
package com.jd.live.agent.plugin.router.springgateway.v3.config;

import com.jd.live.agent.bootstrap.util.Inclusion;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class GatewayConfig {

    public static final String TYPE_REWRITE_PATH_FILTER = "org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory$1";

    public static final String TYPE_STRIP_PREFIX = "org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory$1";

    public static final String TYPE_PREFIX_PATH = "org.springframework.cloud.gateway.filter.factory.PrefixPathGatewayFilterFactory$1";

    public static final String TYPE_SET_PATH = "org.springframework.cloud.gateway.filter.factory.SetPathGatewayFilterFactory$1";

    public static final String TYPE_REACTIVE_LOAD_BALANCE_FILTER = "org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter";

    public static final String TYPE_RETRY_FILTER = "org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory$1";

    // -1=org.springframework.cloud.gateway.filter.NettyWriteResponseFilter
    public static final int DEFAULT_LIVE_FILTER_ORDER = -2;

    private Set<String> pathFilters = new HashSet<>();

    private Set<String> pathFilterPrefixes = new HashSet<>();

    private Set<String> loadBalancerFilters = new HashSet<>();

    private Set<String> retryFilters = new HashSet<>();

    private Set<String> webSchemes = new HashSet<>();

    private int liveFilterOrder = DEFAULT_LIVE_FILTER_ORDER;

    private transient Inclusion inclusion;

    /**
     * Checks if the given name is a path filter.
     *
     * @param filter The filter class name.
     * @return true if the filter is a path filter, false otherwise.
     */
    public boolean isPathFilter(String filter) {
        return filter != null && inclusion != null && inclusion.test(filter);
    }

    /**
     * Checks if the given filter is a load balancer filter.
     *
     * @param filter the filter name to check
     * @return true if it's a load balancer filter, false otherwise
     */
    public boolean isLoadBalancerFilter(String filter) {
        return filter != null && loadBalancerFilters.contains(filter);
    }

    /**
     * Checks if the given filter is a load retry filter.
     *
     * @param filter the filter name to check
     * @return true if it's a retry filter, false otherwise
     */
    public boolean isRetryFilter(String filter) {
        return filter != null && retryFilters.contains(filter);
    }

    public boolean isWebScheme(String scheme) {
        return scheme != null && webSchemes.contains(scheme);
    }

    public void initialize() {
        pathFilters.add(TYPE_REWRITE_PATH_FILTER);
        pathFilters.add(TYPE_STRIP_PREFIX);
        pathFilters.add(TYPE_PREFIX_PATH);
        pathFilters.add(TYPE_SET_PATH);
        loadBalancerFilters.add(TYPE_REACTIVE_LOAD_BALANCE_FILTER);
        retryFilters.add(TYPE_RETRY_FILTER);
        webSchemes.add("http");
        webSchemes.add("https");
        webSchemes.add("http3");
        webSchemes.add("ws");
        webSchemes.add("wss");
        inclusion = new Inclusion(pathFilters, pathFilterPrefixes);
    }

}
