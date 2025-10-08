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
package com.jd.live.agent.plugin.router.springgateway.v3.util;

import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.util.UriUtils;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.netty.Connection;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.jd.live.agent.core.util.StringUtils.choose;
import static com.jd.live.agent.governance.instance.Endpoint.SECURE_SCHEME;
import static com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint.getResponse;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * Utility class for web exchange operations in gateway scenarios.
 */
public class WebExchangeUtils {

    public static URI getURI(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
    }

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
        String scheme = choose(endpoint.getScheme(), overrideScheme);
        URI requestUrl = UriUtils.newURI(uri, scheme, secure, endpoint.getHost(), endpoint.getPort());

        attributes.put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
        attributes.put(GATEWAY_LOADBALANCER_RESPONSE_ATTR, getResponse(endpoint));
    }

    /**
     * Removes and returns an attribute from the server web exchange.
     *
     * @param <T>      the type of the attribute value
     * @param exchange the server web exchange
     * @param key      the attribute key
     * @return the removed attribute value, or null if not found
     */
    public static <T> T removeAttribute(ServerWebExchange exchange, String key) {
        return (T) exchange.getAttributes().remove(key);
    }

    /**
     * Removes multiple attributes from the server web exchange.
     *
     * @param exchange the server web exchange
     * @param keys     the attribute keys to remove
     */
    public static void removeAttributes(ServerWebExchange exchange, String... keys) {
        if (keys != null) {
            Map<String, Object> attributes = exchange.getAttributes();
            for (String key : keys) {
                attributes.remove(key);
            }
        }
    }

    /**
     * Closes the client response connection associated with the exchange.
     *
     * @param exchange the server web exchange
     */
    public static void closeConnection(ServerWebExchange exchange) {
        Connection conn = removeAttribute(exchange, ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR);
        if (conn != null) {
            conn.dispose();
        }
    }

    /**
     * Resets the server web exchange to its initial state.
     *
     * @param exchange the server web exchange to reset
     */
    public static void reset(ServerWebExchange exchange) {
        ServerWebExchangeUtils.reset(exchange);
    }

}
