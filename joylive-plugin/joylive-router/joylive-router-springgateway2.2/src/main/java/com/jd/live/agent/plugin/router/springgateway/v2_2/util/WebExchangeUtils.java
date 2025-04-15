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
package com.jd.live.agent.plugin.router.springgateway.v2_2.util;

import com.jd.live.agent.governance.registry.ServiceEndpoint;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.jd.live.agent.governance.instance.Endpoint.SECURE_SCHEME;
import static com.jd.live.agent.plugin.router.springcloud.v2_2.util.UriUtils.newURI;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * Utility class for web exchange operations in gateway scenarios.
 */
public class WebExchangeUtils {

    /**
     * Forwards the exchange to the specified endpoint while preserving original request information.
     *
     * @param exchange the current web exchange (must not be null)
     * @param endpoint the target service endpoint (must not be null)
     */
    @SuppressWarnings("unchecked")
    public static void forward(ServerWebExchange exchange, ServiceEndpoint endpoint) {
        Map<String, Object> attributes = exchange.getAttributes();

        URI uri = (URI) attributes.getOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
        // preserve the original url
        Set<URI> urls = (Set<URI>) attributes.computeIfAbsent(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, s -> new LinkedHashSet<>());
        urls.add(uri);

        // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
        // if the loadbalancer doesn't provide one.
        String overrideScheme = endpoint.isSecure() ? "https" : "http";
        String schemePrefix = (String) attributes.get(GATEWAY_SCHEME_PREFIX_ATTR);
        if (schemePrefix != null) {
            overrideScheme = uri.getScheme();
        }

        boolean secure = SECURE_SCHEME.test(overrideScheme) || endpoint.isSecure();
        String scheme = endpoint.getScheme();
        scheme = scheme == null ? overrideScheme : scheme;
        URI requestUrl = newURI(uri, scheme, secure, endpoint.getHost(), endpoint.getPort());
        attributes.put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
    }

}
