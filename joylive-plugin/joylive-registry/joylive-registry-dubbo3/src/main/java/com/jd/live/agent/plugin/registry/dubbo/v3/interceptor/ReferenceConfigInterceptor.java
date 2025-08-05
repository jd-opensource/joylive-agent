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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.registry.RegisterMode;
import com.jd.live.agent.governance.registry.RegisterType;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceId;
import org.apache.dubbo.config.ReferenceConfig;

import java.util.Map;

/**
 * ReferenceConfigInterceptor
 */
public class ReferenceConfigInterceptor extends AbstractConfigInterceptor<ReferenceConfig<?>> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConfigInterceptor.class);
    private static final String KEY_REFERENCE_MIGRATION = "migration.step";
    private static final String KEY_APPLICATION_MIGRATION = "dubbo.application.service-discovery.migration";
    private static final String FORCE_INTERFACE = "FORCE_INTERFACE";
    private static final String FORCE_APPLICATION = "FORCE_APPLICATION";

    public ReferenceConfigInterceptor(Application application, Registry registry) {
        super(application, registry);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected RegisterType getServiceId(ReferenceConfig<?> config) {
        String providedBy = config.getProvidedBy();
        Map<String, String> parameters = config.getParameters();
        String migration = parameters.get(KEY_REFERENCE_MIGRATION);
        migration = migration == null || migration.isEmpty() ?
                config.getApplication().getScopeModel().getModelEnvironment().getAppConfigMap().get(KEY_APPLICATION_MIGRATION)
                : migration;
        migration = migration == null || migration.isEmpty() ? System.getProperty(KEY_APPLICATION_MIGRATION) : migration;
        if (FORCE_INTERFACE.equalsIgnoreCase(migration)) {
            return new RegisterType(RegisterMode.INTERFACE, config.getInterface(), config.getInterface(), config.getGroup());
        } else if (FORCE_APPLICATION.equalsIgnoreCase(migration)) {
            return new RegisterType(RegisterMode.INSTANCE, providedBy, config.getInterface(), config.getGroup());
        }
        return providedBy == null || providedBy.isEmpty()
                ? new RegisterType(RegisterMode.INTERFACE, config.getInterface(), config.getInterface(), config.getGroup())
                : new RegisterType(RegisterMode.INSTANCE, providedBy, config.getInterface(), config.getGroup());
    }

    @Override
    protected void subscribe(ReferenceConfig<?> config, ServiceId serviceId) {
        registry.subscribe(serviceId);
        logger.info("Found dubbo consumer {}.", serviceId.getUniqueName());
    }

    @Override
    protected Map<String, String> getContext(ExecutableContext ctx) {
        return ((MethodContext) ctx).getResult();
    }

}
