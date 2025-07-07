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
package com.jd.live.agent.plugin.registry.zuul.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;

import java.util.Map;

/**
 * RouteInterceptor
 */
public class RouteInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(RouteInterceptor.class);

    private final Registry registry;

    public RouteInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Map<String, ZuulProperties.ZuulRoute> map = mc.getResult();
        if (map != null) {
            map.values().forEach(route -> {
                String serviceId = route.getServiceId();
                if (serviceId != null && !serviceId.isEmpty() && !registry.isSubscribed(serviceId)) {
                    registry.subscribe(serviceId);
                    logger.info("Found zuul consumer, service: {}", serviceId);
                }
            });
        }
    }
}
