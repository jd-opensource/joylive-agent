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

import com.alipay.sofa.rpc.common.Version;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.interceptor.AbstractRegistryInterceptor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.util.FrameworkVersion;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.choose;

/**
 * RegistryInterceptor
 */
public class RegistryInterceptor extends AbstractRegistryInterceptor {

    public RegistryInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @Override
    protected List<ServiceInstance> getInstances(MethodContext ctx) {
        ProviderConfig<?> config  = ctx.getArgument(0);
        List<ServerConfig> serverConfigs = config.getServer();
        if (config.isRegister() && serverConfigs != null && !serverConfigs.isEmpty()) {
            return toList(serverConfigs, cfg -> ServiceInstance.builder()
                    .interfaceMode(true)
                    .framework(FrameworkVersion.sofaRpc(Version.VERSION))
                    .service(config.getInterfaceId())
                    .group(choose(config.getGroup(), config.getUniqueId()))
                    .version(choose(config.getVersion(), "1.0"))
                    .scheme(cfg.getProtocol())
                    .host(RegistryUtils.getServerHost(cfg))
                    .port(getPort(cfg))
                    .metadata(RegistryUtils.convertProviderToMap(config, cfg))
                    .build());
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
