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
package com.jd.live.agent.plugin.router.springcloud.v2.request;

import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

/**
 * Defines the contract for an HTTP outbound request within a reactive microservices
 * architecture, focusing on integration with Spring Cloud's load balancing features.
 */
public interface SpringClusterRequest extends HttpOutboundRequest {

    /**
     * Obtains a supplier of service instances for load balancing. This supplier is responsible
     * for providing a list of available service instances that the load balancer can use to
     * distribute incoming requests. The implementation of this method is crucial for enabling
     * dynamic service discovery and selection.
     *
     * @return A {@code ServiceInstanceListSupplier} that provides a list of available service instances.
     */
    ServiceInstanceListSupplier getInstanceSupplier();

}

