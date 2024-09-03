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

import com.alipay.sofa.rpc.bootstrap.ProviderBootstrap;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.bootstrap.AgentLifecycle;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.registry.ServiceProtocol;

import java.util.ArrayList;
import java.util.List;

/**
 * RegistryInterceptor
 */
public class RegistryInterceptor extends AbstractRegistryInterceptor {

    public RegistryInterceptor(Application application, AgentLifecycle lifecycle, Registry registry) {
        super(application, lifecycle, registry);
    }

    @Override
    protected ServiceInstance getInstance(MethodContext ctx) {
        ProviderConfig<?> config  = ctx.getArgument(0);
        if (config.isRegister()) {
            List<ServiceProtocol> protocols = new ArrayList<>();
            List<ServerConfig> serverConfigs = config.getServer();
            for (ServerConfig serverConfig : serverConfigs) {
                protocols.add(ServiceProtocol.builder()
                        .schema(serverConfig.getProtocol())
                        .host(RegistryUtils.getServerHost(serverConfig))
                        .port(getPort(serverConfig))
                        .metadata(RegistryUtils.convertProviderToMap(config, serverConfig))
                        .build());
            }
            return ServiceInstance.builder()
                    .type("sofa-rpc")
                    .service(config.getInterfaceId())
                    .group(StringUtils.isEmpty(config.getGroup()) ? config.getUniqueId() : config.getGroup())
                    .version(StringUtils.isEmpty(config.getVersion()) ? "1.0" : config.getVersion())
                    .protocols(protocols)
                    .build();
        }
        return null;
    }

    private int getPort(ServerConfig serverConfig) {
        Integer port = serverConfig.getVirtualPort();
        if (port == null) {
            port = serverConfig.getPort();
        }
        return port;
    }

}
