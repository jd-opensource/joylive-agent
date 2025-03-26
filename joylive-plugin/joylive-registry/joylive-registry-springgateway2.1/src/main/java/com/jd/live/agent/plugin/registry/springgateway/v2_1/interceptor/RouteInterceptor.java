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
package com.jd.live.agent.plugin.registry.springgateway.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.net.URI;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * RouteInterceptor
 */
public class RouteInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(RouteInterceptor.class);

    private static final String SCHEMA_LB = "lb";

    private final Registry registry;

    public RouteInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        RouteDefinition definition = (RouteDefinition) ctx.getArguments()[0];
        URI uri = definition.getUri();
        if (SCHEMA_LB.equals(uri.getScheme())) {
            // the getMetadata method is not exists in spring cloud greenwich
            String service = uri.getHost();
            if (!registry.isSubscribed(service)) {
                registry.subscribe(service);
                logger.info("Found spring cloud gateway consumer, service: {}", service);
            }
        }
    }
}
