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
package com.jd.live.agent.plugin.router.springcloud.v3.util;

import com.jd.live.agent.core.util.http.HttpUtils;
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
        String host = instance.getHost();
        String scheme = instance.getScheme();
        if (scheme == null || scheme.isEmpty()) {
            scheme = uri.getScheme();
            if (scheme == null || scheme.isEmpty()) {
                scheme = DEFAULT_SCHEME;
            }
            if (instance.isSecure() && INSECURE_SCHEME_MAPPINGS.containsKey(scheme)) {
                scheme = INSECURE_SCHEME_MAPPINGS.get(scheme);
            }
        }
        int port = instance.getPort();
        port = port >= 0 ? port : (DEFAULT_SECURE_SCHEME.equals(scheme) ? 443 : 80);

        if (Objects.equals(host, uri.getHost())
                && port == uri.getPort()
                && Objects.equals(scheme, uri.getScheme())) {
            return uri;
        }
        return HttpUtils.newURI(uri, scheme, host, port);
    }
}
