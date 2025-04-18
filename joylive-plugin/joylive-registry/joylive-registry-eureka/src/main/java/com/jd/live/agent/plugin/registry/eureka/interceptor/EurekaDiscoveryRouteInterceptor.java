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
package com.jd.live.agent.plugin.registry.eureka.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;

/**
 * RouteInterceptor
 */
public class EurekaDiscoveryRouteInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(EurekaDiscoveryRouteInterceptor.class);

    private final Registry registry;

    public EurekaDiscoveryRouteInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ServiceInstance instance = ctx.getArgument(0);
        if (instance instanceof EurekaServiceInstance) {
            EurekaServiceInstance esi = (EurekaServiceInstance) instance;
            String service = esi.getServiceId();
            String vipAddress = esi.getInstanceInfo().getVIPAddress();
            String upperVipAddress = vipAddress == null ? null : vipAddress.toUpperCase();
            if (service.equals(upperVipAddress) && !service.equals(vipAddress)) {
                // service is uppercase name
                // vipAddress is original service name, so we need to use vipAddress as service name
                registry.setServiceAlias(service, vipAddress);
            }
        }
    }
}
