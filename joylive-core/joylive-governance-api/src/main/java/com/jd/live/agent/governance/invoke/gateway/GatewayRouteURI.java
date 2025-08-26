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
package com.jd.live.agent.governance.invoke.gateway;

import com.jd.live.agent.core.util.http.HttpUtils;
import lombok.Getter;

import java.net.URI;
import java.util.regex.Pattern;

import static com.jd.live.agent.core.Constants.PREDICATE_LB;

@Getter
public class GatewayRouteURI {

    private static final Pattern SCHEME_PATTERN = Pattern.compile("[a-zA-Z]([a-zA-Z]|\\d|\\+|\\.|-)*:.*");

    private final URI uri;

    private final String scheme;

    private final String host;

    private final int port;

    private final String schemePrefix;

    private final boolean loadBalancer;

    public GatewayRouteURI(String uri) {
        this(URI.create(uri));
    }

    public GatewayRouteURI(URI uri) {
        // improve performance in route construction
        String schemePrefix = null;
        String host = null;
        if (uri.getHost() == null) {
            if (uri.getRawPath() == null) {
                String schemeSpecificPart = uri.getSchemeSpecificPart();
                if (schemeSpecificPart != null && SCHEME_PATTERN.matcher(schemeSpecificPart).matches()) {
                    // has another scheme. such as lb:ws://SleepService
                    schemePrefix = uri.getScheme();
                    uri = URI.create(schemeSpecificPart);
                }
            }
            if (uri.getHost() == null && uri.getAuthority() != null && (PREDICATE_LB.test(uri.getScheme()) || PREDICATE_LB.test(schemePrefix))) {
                // try fixing special service name. such as "lb://SleepService:DEFAULT"
                String uriString = uri.toString();
                host = com.jd.live.agent.core.util.URI.parse(uriString).getHost();
                if (host != null) {
                    URI newUri = HttpUtils.newURI(uri, uri.getScheme(), null, host, null, null, null, null, uriString);
                    uri = newUri != null ? newUri : uri;
                }
            }
        }
        this.uri = uri;
        this.schemePrefix = schemePrefix;
        this.scheme = uri.getScheme();
        this.host = host != null ? host : uri.getHost();
        this.port = uri.getPort();
        this.loadBalancer = PREDICATE_LB.test(scheme) || PREDICATE_LB.test(schemePrefix);
    }

}
