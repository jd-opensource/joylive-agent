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
package com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.context;

import com.jd.live.agent.governance.invoke.cluster.ClusterContext;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.registry.ServiceRegistry;

/**
 * An extension of {@link ClusterContext} specifically designed for cloud-based environments.
 * This interface provides methods to retrieve service registries for dynamically managing
 * and discovering services in a cloud cluster.
 */
public interface CloudClusterContext extends ClusterContext {

    /**
     * Retrieves a {@link ServiceRegistry} for the specified service name.
     * The service registry is used to manage and discover instances of the service
     * within the cloud cluster.
     *
     * @param service the name of the service for which the registry is being retrieved
     * @return a {@link ServiceRegistry} instance for the specified service
     */
    ServiceRegistry getServiceRegistry(String service);

    /**
     * Retrieves the default retry policy for the specified service.
     * The retry policy defines the rules for retrying failed requests to the service,
     * such as the number of retries, backoff strategy, and conditions under which retries should be attempted.
     *
     * @param service the name of the service for which the retry policy is being retrieved
     * @return the default {@link RetryPolicy} instance for the specified service
     */
    default RetryPolicy getDefaultRetryPolicy(String service) {
        return null;
    }
}

