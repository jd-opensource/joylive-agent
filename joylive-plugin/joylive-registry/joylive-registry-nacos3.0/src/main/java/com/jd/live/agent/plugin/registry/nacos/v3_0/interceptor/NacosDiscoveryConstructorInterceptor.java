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
package com.jd.live.agent.plugin.registry.nacos.v3_0.interceptor;

import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;

/**
 * Nacos Discovery Constructor Interceptor.
 *
 * <p>This interceptor adds hooks to the Agent's registry to preemptively warm up
 * service instances when the application is ready. It registers a listener that
 * triggers service instance discovery for all registered services, improving
 * initial response times by pre-loading service information into cache.</p>
 */
public class NacosDiscoveryConstructorInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(NacosDiscoveryConstructorInterceptor.class);

    private final Registry registry;

    public NacosDiscoveryConstructorInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        // warmup subscription for spring cloud
        NacosServiceDiscovery discovery = (NacosServiceDiscovery) ctx.getTarget();
        // trigger on application ready event
        registry.addListener(serviceIds -> {
            Thread thread = new Thread(() -> {
                try {
                    serviceIds.forEach(serviceId -> {
                        try {
                            logger.info("Starting warmup service instances for {}.", serviceId.getService());
                            discovery.getInstances(serviceId.getService());
                        } catch (NacosException e) {
                            logger.error("Error occurs while warmup service instances {}.", serviceId.getService(), e);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Failed to create nacos naming service.", e);
                }
            });
            thread.setDaemon(true);
            thread.start();
        });
    }
}
