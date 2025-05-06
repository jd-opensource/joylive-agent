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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.RegisterMode;
import com.jd.live.agent.governance.registry.RegisterType;
import org.apache.dubbo.config.ReferenceConfig;

import java.util.Map;

/**
 * ServiceConfigInterceptor
 */
public class ReferenceConfigInterceptor extends AbstractConfigInterceptor<ReferenceConfig<?>> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConfigInterceptor.class);

    public ReferenceConfigInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected void subscribe(String service, String group) {
        registry.subscribe(service, group);
        logger.info("Found dubbo consumer, service: {}, group: {}", service, group);
    }

    @Override
    protected Map<String, String> getContext(ExecutableContext ctx) {
        return ctx.getArgument(0);
    }

    @Override
    protected RegisterType getRegistryType(String interfaceName, ReferenceConfig<?> config) {
        String service = config.getProvidedBy();
        return service == null || service.isEmpty()
                ? new RegisterType(RegisterMode.INTERFACE, interfaceName, interfaceName)
                : new RegisterType(RegisterMode.INSTANCE, service, interfaceName);
    }
}
