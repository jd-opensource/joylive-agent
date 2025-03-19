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
package com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.context;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.registry.ServiceRegistry;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import lombok.Getter;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract implementation of CloudClusterContext for load balancing and service instance management
 * Provides core functionalities for load balancer property retrieval and service instance suppliers
 */
public abstract class AbstractCloudClusterContext implements CloudClusterContext {

    @Getter
    protected ServiceRegistryFactory registryFactory;

    protected RetryPolicy defaultRetryPolicy;

    @Override
    public boolean isRetryable() {
        return defaultRetryPolicy != null;
    }

    @Override
    public RetryPolicy getDefaultRetryPolicy(String service) {
        return defaultRetryPolicy;
    }

    @Override
    public ServiceRegistry getServiceRegistry(String service) {
        return registryFactory == null ? null : registryFactory.getServiceRegistry(service);
    }

    /**
     * Creates a RetryPolicy based on the provided LoadBalancerRetryProperties.
     *
     * @param properties the LoadBalancerRetryProperties object containing the retry configuration
     * @return a RetryPolicy instance, or null if retry is not enabled
     */
    protected static RetryPolicy getDefaultRetryPolicy(LoadBalancerRetryProperties properties) {
        if (properties == null || !properties.isEnabled()) {
            return null;
        }
        RetryPolicy retryPolicy = new RetryPolicy();
        Set<Integer> codes = properties.getRetryableStatusCodes();
        Set<String> statuses = new HashSet<>(codes.size());
        codes.forEach(status -> statuses.add(String.valueOf(status)));
        retryPolicy.setRetry(properties.getMaxRetriesOnNextServiceInstance());
        retryPolicy.setInterval(properties.getBackoff().getMinBackoff().toMillis());
        retryPolicy.setErrorCodes(statuses);
        if (!properties.isRetryOnAllOperations()) {
            Set<String> methods = new HashSet<>(1);
            methods.add(HttpMethod.GET.name());
            retryPolicy.setMethods(methods);
        }
        return retryPolicy;
    }

}
