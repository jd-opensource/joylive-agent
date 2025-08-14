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
package com.jd.live.agent.plugin.router.springcloud.v1.util;

import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class for modifying URIs to redirect requests to specific service instances.
 */
public class UriUtils {

    /**
     * The default scheme used when no scheme is specified.
     */
    private static final String DEFAULT_SCHEME = "http";

    /**
     * The default secure scheme used when no secure scheme is specified.
     */
    private static final String DEFAULT_SECURE_SCHEME = "https";

    /**
     * A map of insecure scheme mappings to their secure counterparts.
     */
    private static final Map<String, String> INSECURE_SCHEME_MAPPINGS;

    static {
        INSECURE_SCHEME_MAPPINGS = new HashMap<>();
        INSECURE_SCHEME_MAPPINGS.put(DEFAULT_SCHEME, DEFAULT_SECURE_SCHEME);
        INSECURE_SCHEME_MAPPINGS.put("ws", "wss");
    }

    /**
     * Modifies the URI in order to redirect the request to a service instance of choice.
     *
     * @param instance the {@link ServiceInstance} to redirect the request to.
     * @param uri      the {@link URI} from the uri request
     * @return the modified {@link URI}
     */
    public static URI newURI(ServiceInstance instance, URI uri) {
        return newURI(uri, null, instance.isSecure(), instance.getHost(), instance.getPort());
    }

    /**
     * Modifies the URI in order to redirect the request to a service instance of choice.
     *
     * @param endpoint the {@link ServiceEndpoint} to redirect the request to.
     * @param uri      the {@link URI} from the uri request
     * @return the modified {@link URI}
     */
    public static URI newURI(ServiceEndpoint endpoint, URI uri) {
        return newURI(uri, endpoint.getScheme(), endpoint.isSecure(), endpoint.getHost(), endpoint.getPort());
    }

    /**
     * Creates a new URI by modifying components of an existing URI.
     *
     * @param uri    the original URI to base the new URI on (must not be null)
     * @param scheme the desired scheme (nullable, falls back to original URI's scheme or {@code DEFAULT_SCHEME})
     * @param secure whether to use secure scheme mapping (e.g., map http→https)
     * @param host   the new host (nullable, falls back to original URI's host)
     * @param port   the new port (negative values will use scheme-specific defaults)
     * @return a new URI with specified modifications, or original URI if no changes were needed
     */
    public static URI newURI(URI uri, String scheme, boolean secure, String host, int port) {
        if (scheme == null || scheme.isEmpty()) {
            scheme = uri.getScheme();
            if (scheme == null || scheme.isEmpty()) {
                scheme = DEFAULT_SCHEME;
            }
        }
        if (secure) {
            String secureScheme = INSECURE_SCHEME_MAPPINGS.get(scheme);
            if (secureScheme != null) {
                scheme = secureScheme;
            }
        }
        port = port >= 0 ? port : (DEFAULT_SECURE_SCHEME.equals(scheme) ? 443 : 80);

        if (Objects.equals(host, uri.getHost()) && port == uri.getPort() && Objects.equals(scheme, uri.getScheme())) {
            return uri;
        }
        return HttpUtils.newURI(uri, scheme, host, port);
    }


}
