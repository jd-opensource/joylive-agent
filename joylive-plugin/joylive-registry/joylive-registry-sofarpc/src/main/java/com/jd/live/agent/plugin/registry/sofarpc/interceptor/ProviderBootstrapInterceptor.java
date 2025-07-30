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
package com.jd.live.agent.plugin.registry.sofarpc.interceptor;

import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.bootstrap.ProviderBootstrap;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.doc.DocumentRegistry;
import com.jd.live.agent.governance.doc.ServiceAnchor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceId;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * ProviderBootstrapInterceptor
 */
public class ProviderBootstrapInterceptor extends AbstractBootstrapInterceptor<ProviderConfig<?>> {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerBootstrapInterceptor.class);

    private final DocumentRegistry docRegistry;

    public ProviderBootstrapInterceptor(Application application, Registry registry, DocumentRegistry docRegistry) {
        super(application, registry);
        this.docRegistry = docRegistry;
    }

    @Override
    protected void subscribe(ProviderConfig<?> config) {
        ServiceId serviceId = new ServiceId(config.getInterfaceId(), getGroup(config), true);
        registry.register(serviceId);
        logger.info("Found sofa rpc provider {}.", serviceId.getUniqueName());
        if (docRegistry.isEnabled()) {
            Class<?> clazz = config.getProxyClass();
            if (clazz != GenericService.class) {
                docRegistry.register(() -> {
                    List<ServiceAnchor> anchors = new ArrayList<>(16);
                    Method[] methods = clazz.getMethods();
                    for (Method method : methods) {
                        anchors.add(new ServiceAnchor(serviceId.getService(), serviceId.getGroup(), "/", method.getName()));
                    }
                    return anchors;
                });
            }
        }
    }

    @Override
    protected ProviderConfig<?> getConfig(ExecutableContext ctx) {
        return ((ProviderBootstrap<?>) ctx.getTarget()).getProviderConfig();
    }
}
