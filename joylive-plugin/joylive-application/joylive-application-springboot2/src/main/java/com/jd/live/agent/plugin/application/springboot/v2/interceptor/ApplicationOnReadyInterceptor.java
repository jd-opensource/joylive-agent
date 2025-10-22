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
package com.jd.live.agent.plugin.application.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.instance.AppService;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.util.FrameworkVersion;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.v2.util.AppLifecycle;
import com.jd.live.agent.plugin.application.springboot.v2.util.SpringUtils;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortInfo;
import org.springframework.boot.SpringBootVersion;

import java.util.HashMap;
import java.util.Map;

public class ApplicationOnReadyInterceptor extends InterceptorAdaptor {

    private final AppListener listener;

    private final GovernanceConfig config;

    private final Registry registry;

    private final Application application;

    public ApplicationOnReadyInterceptor(AppListener listener, GovernanceConfig config, Registry registry, Application application) {
        this.listener = listener;
        this.config = config;
        this.registry = registry;
        this.application = application;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // fix for spring boot 2.1, it will trigger twice.
        AppLifecycle.ready(() -> {
            SpringAppContext context = new SpringAppContext(ctx.getArgument(0));
            if (config.getRegistryConfig().isRegisterAppServiceEnabled()) {
                registry.register(createInstance(context, application.getService()));
            }
            listener.onReady(context);
        });
    }

    /**
     * Creates a service instance with configuration derived from the application context.
     * Automatically detects host and port (using multiple strategies), collects application
     * labels as metadata, and embeds framework version information.
     *
     * @param context    Application context for environment and port detection
     * @param appService Service metadata provider
     * @return Configured service instance ready for registration
     */
    private ServiceInstance createInstance(AppContext context, AppService appService) {
        String address = context.getProperty("server.address");
        address = address == null || address.isEmpty() ? Ipv4.getLocalIp() : address;
        PortInfo port = SpringUtils.getPort(context);
        ServiceInstance instance = new ServiceInstance();
        instance.setService(appService.getName());
        instance.setGroup(appService.getGroup());
        instance.setHost(address);
        Map<String, String> metadata = new HashMap<>();
        application.labelRegistry(metadata::putIfAbsent);
        metadata.put(Constants.LABEL_FRAMEWORK, FrameworkVersion.springBoot(SpringBootVersion.getVersion()).toString());
        if (port != null) {
            instance.setPort(port.getPort());
            instance.setId(instance.getAddress());
            if (port.isSecure()) {
                metadata.put(Constants.LABEL_SECURE, String.valueOf(port.isSecure()));
            }
        } else {
            instance.setId(application.getInstance());
        }
        instance.setMetadata(metadata);
        return instance;
    }
}
