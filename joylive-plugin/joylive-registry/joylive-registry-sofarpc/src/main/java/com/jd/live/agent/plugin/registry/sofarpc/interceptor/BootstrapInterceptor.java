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

import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.PolicyType;

/**
 * BootstrapInterceptor provides a base class for interceptors during the bootstrap phase.
 */
public abstract class BootstrapInterceptor extends InterceptorAdaptor {

    /**
     * Logger for logging information.
     */
    private static final Logger logger = LoggerFactory.getLogger(BootstrapInterceptor.class);

    /**
     * Application instance.
     */
    protected final Application application;

    /**
     * Supplier for policies.
     */
    protected final PolicySupplier policySupplier;

    /**
     * Constructs a new BootstrapInterceptor with the specified application and policy supplier.
     *
     * @param application    the application instance
     * @param policySupplier the supplier for policies
     */
    public BootstrapInterceptor(Application application, PolicySupplier policySupplier) {
        this.application = application;
        this.policySupplier = policySupplier;
    }

    /**
     * Attaches tags to the specified configuration.
     *
     * @param config the configuration to which tags will be attached
     * @param <T>    the service interface type
     * @param <S>    the specific type of the AbstractInterfaceConfig
     */
    protected <T, S extends AbstractInterfaceConfig<T, S>> void attachTags(S config) {
        application.label(config::setParameter);
        if (logger.isInfoEnabled()) {
            logger.info("Success filling metadata for registration " + config.getInterfaceId() + " in " + config.getClass());
        }
    }

    /**
     * Subscribes to policy for the specified configuration.
     *
     * @param config the configuration for which policy will be subscribed
     * @param <T>    the service interface type
     * @param <S>    the specific type of the AbstractInterfaceConfig
     */
    protected <T, S extends AbstractInterfaceConfig<T, S>> void subscribePolicy(S config) {
        // TODO: Perform asynchronously, hook at application startup to ensure policy synchronization is complete
        // This method can obtain the target interface name for generic calls
        String serviceName = config.getInterfaceId();
        policySupplier.subscribe(serviceName, PolicyType.SERVICE_POLICY);
    }
}
