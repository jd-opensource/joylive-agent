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
package com.jd.live.agent.plugin.router.springgateway.v4.filter;

import lombok.Getter;

import java.net.URI;
import java.util.regex.Pattern;

@Getter
public class LiveRouteURI {

    public static final String SCHEMA_LB = "lb";

    private static final Pattern SCHEME_PATTERN = Pattern.compile("[a-zA-Z]([a-zA-Z]|\\d|\\+|\\.|-)*:.*");

    private final URI uri;

    private final String schemePrefix;

    private final boolean loadBalancer;

    public LiveRouteURI(URI uri) {
        // improve performance in route construction
        String schemePrefix = null;
        boolean hasAnotherScheme = uri != null
                && uri.getHost() == null
                && uri.getRawPath() == null
                && SCHEME_PATTERN.matcher(uri.getSchemeSpecificPart()).matches();
        if (hasAnotherScheme) {
            schemePrefix = uri.getScheme();
            uri = URI.create(uri.getSchemeSpecificPart());
        }
        this.uri = uri;
        this.schemePrefix = schemePrefix;
        this.loadBalancer = SCHEMA_LB.equals(uri.getScheme()) || SCHEMA_LB.equals(schemePrefix);
    }

    public String getScheme() {
        return uri.getScheme();
    }

    public String getHost() {
        return uri.getHost();
    }

    public int getPort() {
        return uri.getPort();
    }
}
