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
package com.jd.live.agent.plugin.registry.springcloud.v2_2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import org.springframework.boot.SpringBootVersion;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.util.HashMap;
import java.util.Map;

/**
 * RegistryInterceptor
 */
public class RegistryInterceptor extends AbstractRegistryInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RegistryInterceptor.class);

    public RegistryInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected void beforeRegister(MethodContext ctx) {
        Registration registration = ctx.getArgument(0);
        Map<String, String> metadata = registration.getMetadata();
        if (metadata != null) {
            application.labelRegistry(metadata::putIfAbsent, true);
            metadata.put(Constants.LABEL_FRAMEWORK, "spring-boot-" + SpringBootVersion.getVersion());
            if (registration.isSecure()) {
                metadata.put(Constants.LABEL_SECURE, String.valueOf(registration.isSecure()));
            }
        }
        // subscribe policy
        String serviceId = registration.getServiceId();
        registry.register(serviceId);
        logger.info("Found spring cloud provider, service: {}", serviceId);
    }

    @Override
    protected ServiceInstance getInstance(MethodContext ctx) {
        Registration registration = (Registration) ctx.getArguments()[0];
        Map<String, String> metadata = registration.getMetadata();
        metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        return ServiceInstance.builder()
                .interfaceMode(false)
                .framework("spring-cloud.v2_2")
                .service(registration.getServiceId())
                .group(metadata.get(Constants.LABEL_SERVICE_GROUP))
                .scheme(registration.getScheme())
                .host(registration.getHost())
                .port(registration.getPort())
                .build();
    }
}
