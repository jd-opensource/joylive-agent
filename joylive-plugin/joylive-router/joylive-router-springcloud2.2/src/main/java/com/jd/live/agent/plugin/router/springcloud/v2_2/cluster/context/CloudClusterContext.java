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

import com.jd.live.agent.governance.invoke.cluster.ClusterContext;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

public interface CloudClusterContext extends ClusterContext {

    RetryPolicy getDefaultRetryPolicy();

    /**
     * Obtains service instance provider for discovery operations
     *
     * @param service Target service identifier/name
     * @return Supplier of available service instances
     */
    ServiceInstanceListSupplier getServiceInstanceListSupplier(String service);

}
