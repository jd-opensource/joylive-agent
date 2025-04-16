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
package com.jd.live.agent.plugin.router.springcloud.v4.cluster.context;

import com.jd.live.agent.governance.invoke.cluster.ClusterContext;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Extended cluster context with cloud-specific capabilities.
 */
public interface CloudClusterContext extends ClusterContext {

    /**
     * Resolves endpoints for a service request.
     *
     * @param request the service request
     * @return matching endpoints
     */
    CompletionStage<List<ServiceEndpoint>> getEndpoints(ServiceRequest request);

    /**
     * Retrieves the service context for the specified service name.
     *
     * @param service the name of the service to lookup
     * @return the associated service context
     */
    ServiceContext getServiceContext(String service);

}
