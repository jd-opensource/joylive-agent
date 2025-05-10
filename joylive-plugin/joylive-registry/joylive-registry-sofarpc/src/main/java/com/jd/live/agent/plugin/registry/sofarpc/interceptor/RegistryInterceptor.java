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

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * RegistryInterceptor
 */
public class RegistryInterceptor extends AbstractRegistryInterceptor {

    public RegistryInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected void doRegister(MethodContext mc) {
        List<ServiceInstance> instances = getInstances(mc);
        if (instances != null) {
            registry.register(instances, () -> {
                mc.invokeOrigin();
                return null;
            });
            mc.setSkip(true);
        }
    }

    /**
     * Converts provider configuration into service instances.
     *
     * @param ctx the method context containing provider configuration
     * @return list of service instances, or null if registration should be skipped
     */
    private List<ServiceInstance> getInstances(MethodContext ctx) {
        List<ServiceInstance> instances = null;
        ProviderConfig<?> config  = ctx.getArgument(0);
        List<ServerConfig> serverConfigs = config.getServer();
        if (config.isRegister() && serverConfigs != null && !serverConfigs.isEmpty()) {
            instances = new ArrayList<>(serverConfigs.size());
            for (ServerConfig serverConfig : serverConfigs) {
                instances.add(
                        ServiceInstance.builder()
                                .type("sofa-rpc")
                                .service(config.getInterfaceId())
                                .group(StringUtils.isEmpty(config.getGroup()) ? config.getUniqueId() : config.getGroup())
                                .version(StringUtils.isEmpty(config.getVersion()) ? "1.0" : config.getVersion())
                                .scheme(serverConfig.getProtocol())
                                .host(RegistryUtils.getServerHost(serverConfig))
                                .port(getPort(serverConfig))
                                .metadata(RegistryUtils.convertProviderToMap(config, serverConfig))
                                .build());
            }
        }
        return null;
    }

    /**
     * Gets the effective port number from server configuration.
     * Prefers virtual port if specified, falls back to regular port otherwise.
     *
     * @param serverConfig the server configuration
     * @return the effective port number
     */
    private int getPort(ServerConfig serverConfig) {
        Integer port = serverConfig.getVirtualPort();
        if (port == null) {
            port = serverConfig.getPort();
        }
        return port;
    }

}
