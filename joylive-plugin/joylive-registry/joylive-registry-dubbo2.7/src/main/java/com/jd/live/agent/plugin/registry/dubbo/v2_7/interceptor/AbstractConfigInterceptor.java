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
import com.jd.live.agent.governance.registry.Registry;
import org.apache.dubbo.config.AbstractInterfaceConfig;

import java.util.Map;

import static com.jd.live.agent.governance.util.Predicates.isDubboSystemService;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;

/**
 * AbstractConfigInterceptor
 */
public abstract class AbstractConfigInterceptor<T extends AbstractInterfaceConfig> extends InterceptorAdaptor {

    protected static final int REGISTRY_TYPE_SERVICE = 2;

    protected static final int REGISTRY_TYPE_INTERFACE = 1;

    protected static final int REGISTRY_TYPE_ALL = REGISTRY_TYPE_SERVICE | REGISTRY_TYPE_INTERFACE;

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
        String service = getService(config);
        Map<String, String> map = getContext(ctx);
        if (!isDubboSystemService(service)) {
            subscribe(service, config, map);
        }
    }

    /**
     * Subscribes to a specific service with the specified configuration and context.
     *
     * @param service the name of the service to register.
     * @param config  the configuration object for the service, containing details such as application name and group.
     * @param ctx     the context map to populate with the service group and registry type.
     */
    protected void subscribe(String service, T config, Map<String, String> ctx) {
        application.labelRegistry(ctx::putIfAbsent);
        int type = getRegistryType(config);
        switch (type) {
            case REGISTRY_TYPE_SERVICE:
                ctx.put(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE);
                subscribe(config.getApplication().getName(), config.getGroup());
                break;
            case REGISTRY_TYPE_ALL:
                ctx.put(REGISTRY_TYPE_KEY, "all");
                subscribe(config.getApplication().getName(), config.getGroup());
                subscribe(service, config.getGroup());
            case REGISTRY_TYPE_INTERFACE:
            default:
                subscribe(service, config.getGroup());
        }
    }

    /**
     * Subscribes to a specific service in the specified group.
     * This method must be implemented by subclasses to define the subscription logic.
     *
     * @param service the name of the service to subscribe to.
     * @param group   the group to which the service belongs.
     */
    protected abstract void subscribe(String service, String group);

    /**
     * Gets the context map for the given executable context.
     *
     * @param ctx the executable context for which to get the context map.
     * @return the context map for the given executable context.
     */
    protected abstract Map<String, String> getContext(ExecutableContext ctx);

    /**
     * Gets the service name from the given configuration object.
     *
     * @param config the configuration object for the service.
     * @return the name of the service.
     */
    protected String getService(T config) {
        return config.getInterface();
    }

    /**
     * Gets the registry type for the given configuration object.
     *
     * @param config the configuration object for the service.
     * @return the registry type for the service.
     */
    protected abstract int getRegistryType(T config);

}
