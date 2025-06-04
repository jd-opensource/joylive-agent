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
package com.jd.live.agent.plugin.registry.grpc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceId;

/**
 * DiscoveryClientConstructorInterceptor
 */
public class DiscoveryClientConstructorInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryClientConstructorInterceptor.class);

    private final Registry registry;

    private final Application application;

    public DiscoveryClientConstructorInterceptor(Registry registry, Application application) {
        this.registry = registry;
        this.application = application;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        String service = ctx.getArgument(0);
        ServiceId serviceId = new ServiceId(service, null, true);
        if (!application.isReady()) {
            registry.subscribe(serviceId);
        } else {
            // gRPC is triggered on the first request
            registry.subscribe(serviceId, (message, e) -> {
                logger.warn(message, e);
                return null;
            });
        }
        logger.info("Found grpc consumer, service: {}", service);
    }
}
