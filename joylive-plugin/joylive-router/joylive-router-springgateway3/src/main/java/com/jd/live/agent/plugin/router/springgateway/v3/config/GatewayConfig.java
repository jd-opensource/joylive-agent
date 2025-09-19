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

    private Set<String> pathFilters = new HashSet<>();

    private Set<String> pathFilterPrefixes = new HashSet<>();

    private Set<String> webSchemes = new HashSet<>();

    private transient Inclusion inclusion;

    /**
     * Checks if the given name is a path filter.
     *
     * @param filter The filter class name.
     * @return true if the filter is a path filter, false otherwise.
     */
    public boolean isPathFilter(String filter) {
        return inclusion != null && inclusion.test(filter);
    }

    public boolean isWebScheme(String scheme) {
        return webSchemes != null && scheme != null && webSchemes.contains(scheme.toLowerCase());
    }

    public void initialize() {
        pathFilters.add(TYPE_REWRITE_PATH_FILTER);
        pathFilters.add(TYPE_STRIP_PREFIX);
        pathFilters.add(TYPE_PREFIX_PATH);
        pathFilters.add(TYPE_SET_PATH);
        webSchemes.add("http");
        webSchemes.add("https");
        webSchemes.add("http3");
        webSchemes.add("ws");
        webSchemes.add("wss");
        inclusion = new Inclusion(pathFilters, pathFilterPrefixes);
    }

}
