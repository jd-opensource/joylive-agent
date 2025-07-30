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
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.RegisterMode;
import com.jd.live.agent.governance.registry.RegisterType;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceId;
import org.apache.dubbo.config.AbstractInterfaceConfig;

import java.util.Map;

import static com.jd.live.agent.governance.util.Predicates.isDubboSystemService;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;

/**
 * AbstractConfigInterceptor
 */
public abstract class AbstractConfigInterceptor<T extends AbstractInterfaceConfig> extends InterceptorAdaptor {

    protected final Application application;

    protected final Registry registry;

    public AbstractConfigInterceptor(Application application, Registry registry) {
        this.application = application;
        this.registry = registry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        T config = (T) ctx.getTarget();
        // Fix for dubbo 2.7.8
        RegisterType info = getRegisterType(config);
        Map<String, String> map = getContext(ctx);
        if (!isDubboSystemService(info.getInterfaceName())) {
            subscribe(config, info, map);
        }
    }

    protected abstract RegisterType getRegisterType(T config);

    /**
     * Subscribes to a service using the specified configuration and registration info.
     * Populates the context map with service metadata and handles different registration modes.
     *
     * @param config the interface configuration
     * @param info   the service registration details (name, group, mode)
     * @param ctx    the context map to store registry type and group information
     */
    protected void subscribe(T config, RegisterType info, Map<String, String> ctx) {
        application.labelRegistry(ctx::putIfAbsent);
        RegisterMode mode = info.getMode();
        switch (mode) {
            case INSTANCE:
                ctx.put(REGISTRY_TYPE_KEY, mode.getName());
                subscribe(config, new ServiceId(info.getService(), info.getGroup(), false));
                break;
            case ALL:
                ctx.put(REGISTRY_TYPE_KEY, mode.getName());
                subscribe(config, new ServiceId(info.getService(), info.getGroup(), false));
                subscribe(config, new ServiceId(info.getInterfaceName(), info.getGroup(), true));
                break;
            case INTERFACE:
            default:
                subscribe(config, new ServiceId(info.getInterfaceName(), info.getGroup(), true));

        }
    }

    /**
     * Subscribes to the specified service within its group.
     *
     * @param config    the interface config
     * @param serviceId the unique identifier of the service to subscribe to
     */
    protected abstract void subscribe(T config, ServiceId serviceId);

    /**
     * Gets the context map for the given executable context.
     *
     * @param ctx the executable context for which to get the context map.
     * @return the context map for the given executable context.
     */
    protected abstract Map<String, String> getContext(ExecutableContext ctx);

}
