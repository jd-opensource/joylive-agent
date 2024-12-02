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
package com.jd.live.agent.plugin.registry.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.bootstrap.AgentLifecycle;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.registry.ServiceProtocol;
import org.springframework.boot.SpringBootVersion;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RegistryInterceptor
 */
public class RegistryInterceptor extends AbstractRegistryInterceptor {

    private final PolicySupplier policySupplier;

    public RegistryInterceptor(Application application, AgentLifecycle lifecycle, Registry registry, PolicySupplier policySupplier) {
        super(application, lifecycle, registry);
        this.policySupplier = policySupplier;
    }

    /**
     * Enhanced logic before method execution
     *
     * @see org.springframework.cloud.client.serviceregistry.ServiceRegistry#register(Registration)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        Registration registration = (Registration) ctx.getArguments()[0];
        Map<String, String> metadata = registration.getMetadata();
        if (metadata != null) {
            application.labelRegistry(metadata::putIfAbsent, true);
            metadata.put(Constants.LABEL_FRAMEWORK, "spring-boot-" + SpringBootVersion.getVersion());
        }
        policySupplier.subscribe(registration.getServiceId());
        super.onEnter(ctx);
    }

    @Override
    protected ServiceInstance getInstance(MethodContext ctx) {
        Registration registration = (Registration) ctx.getArguments()[0];
        Map<String, String> metadata = registration.getMetadata();
        metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        return ServiceInstance.builder()
                .type("spring-cloud.v3")
                .service(registration.getServiceId())
                .group(metadata.get(Constants.LABEL_SERVICE_GROUP))
                .host(registration.getHost())
                .port(registration.getPort())
                .protocols(Collections.singletonList(
                        ServiceProtocol.builder()
                                .schema(registration.getScheme())
                                .host(registration.getHost())
                                .port(registration.getPort())
                                .url(DefaultServiceInstance.getUri(registration).toString())
                                .metadata(metadata)
                                .build()))
                .build();
    }
}
