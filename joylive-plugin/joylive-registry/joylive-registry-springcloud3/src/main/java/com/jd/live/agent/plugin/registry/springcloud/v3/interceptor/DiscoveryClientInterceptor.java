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
package com.jd.live.agent.plugin.registry.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.PolicyType;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ServiceRegistryInterceptor
 */
public class DiscoveryClientInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryClientInterceptor.class);

    private final PolicySupplier policySupplier;

    public DiscoveryClientInterceptor(PolicySupplier policySupplier) {
        this.policySupplier = policySupplier;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        ServiceInstanceListSupplier supplier = (ServiceInstanceListSupplier) ctx.getTarget();
        subscribePolicy(supplier.getServiceId());
    }

    private void subscribePolicy(String serviceId) {
        try {
            policySupplier.subscribe(serviceId, PolicyType.SERVICE_POLICY).get(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            logger.warn("Failed to get governance policy for " + serviceId + ", caused by " + e.getMessage(), e);
        } catch (TimeoutException e) {
            logger.warn("Failed to get governance policy for " + serviceId + ", caused by it's timeout.");
        }
    }
}
