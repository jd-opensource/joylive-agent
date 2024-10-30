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
package com.jd.live.agent.plugin.registry.dubbo.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
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

    protected final PolicySupplier policySupplier;

    public AbstractConfigInterceptor(Application application, PolicySupplier policySupplier) {
        this.application = application;
        this.policySupplier = policySupplier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        T config = (T) ctx.getTarget();
        String service = getService(config);
        Map<String, String> map = getContext(ctx);
        if (!isDubboSystemService(service)) {
            register(service, config, map);
        }
    }

    /**
     * Registers a service with the specified configuration and context.
     * <p>
     * This method registers a service with the application's label registry and sets up the necessary context for the service.
     * The context is populated with the service group and registry type, which can be either "service", "all", or "interface".
     * The method also subscribes to the policy supplier for the service or its application, depending on the registry type.
     * </p>
     *
     * @param service the name of the service to register.
     * @param config  the configuration object for the service.
     * @param ctx     the context map to populate with the service group and registry type.
     */
    private void register(String service, T config, Map<String, String> ctx) {
        application.labelRegistry(ctx::putIfAbsent);
        int type = getRegistryType(config);
        switch (type) {
            case REGISTRY_TYPE_SERVICE:
                ctx.put(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE);
                policySupplier.subscribe(config.getApplication().getName());
                break;
            case REGISTRY_TYPE_ALL:
                ctx.put(REGISTRY_TYPE_KEY, "all");
                policySupplier.subscribe(config.getApplication().getName());
                policySupplier.subscribe(service);
            case REGISTRY_TYPE_INTERFACE:
            default:
                policySupplier.subscribe(service);

        }
    }

    /**
     * Gets the context map for the given executable context.
     * <p>
     * This method is called by the {@code register} method to get the context map for the service being registered.
     * The context map is used to store the service group and registry type.
     * </p>
     *
     * @param ctx the executable context for which to get the context map.
     * @return the context map for the given executable context.
     */
    protected abstract Map<String, String> getContext(ExecutableContext ctx);

    /**
     * Gets the service name from the given configuration object.
     * <p>
     * This method is called by the {@code register} method to get the name of the service being registered.
     * </p>
     *
     * @param config the configuration object for the service.
     * @return the name of the service.
     */
    protected String getService(T config) {
        return config.getInterface();
    }

    /**
     * Gets the registry type for the given configuration object.
     * <p>
     * This method is called by the {@code register} method to determine the registry type for the service being registered.
     * The registry type can be either "service", "all", or "interface".
     * </p>
     *
     * @param config the configuration object for the service.
     * @return the registry type for the service.
     */
    protected abstract int getRegistryType(T config);

}
