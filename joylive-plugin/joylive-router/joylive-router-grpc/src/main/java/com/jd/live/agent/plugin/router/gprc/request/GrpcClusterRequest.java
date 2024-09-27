///*
// * Copyright © ${year} ${owner} (${email})
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.jd.live.agent.plugin.router.gprc.request;
//
//import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
//
//import java.util.function.Consumer;
//
///**
// * Defines the contract for an HTTP outbound request within a reactive microservices
// * architecture, focusing on integration with Spring Cloud's load balancing features.
// */
//public interface GrpcClusterRequest extends HttpOutboundRequest {
//
//    /**
//     * Retrieves the load balancer request object that encapsulates the original request
//     * data along with any hints that may influence load balancing decisions. This object
//     * is used by the load balancer to select an appropriate service instance based on the
//     * provided hints and other criteria.
//     *
//     * @return A {@code Request<?>} object containing the context for the load balancing operation.
//     */
//    Request<?> getLbRequest();
//
//    /**
//     * Gets the properties associated with the load balancing operation. These properties
//     * may include configurations and hints that help tailor the load balancing behavior
//     * to the needs of the specific request or service.
//     *
//     * @return An instance of {@code LoadBalancerProperties} containing load balancing configuration.
//     */
//    LoadBalancerProperties getProperties();
//
//    /**
//     * Obtains a supplier of service instances for load balancing. This supplier is responsible
//     * for providing a list of available service instances that the load balancer can use to
//     * distribute incoming requests. The implementation of this method is crucial for enabling
//     * dynamic service discovery and selection.
//     *
//     * @return A {@code ServiceInstanceListSupplier} that provides a list of available service instances.
//     */
//    ServiceInstanceListSupplier getInstanceSupplier();
//
//    /**
//     * Retrieves the request data that will be used by the load balancer to make service instance
//     * selection decisions. This data typically includes the original request information and any
//     * additional metadata or hints relevant to load balancing.
//     *
//     * @return An instance of {@code RequestData} representing the data of the original request.
//     */
//    RequestData getRequestData();
//
//    /**
//     * Executes custom logic across the set of lifecycle processors associated with the load balancer,
//     * allowing for enhanced control and monitoring of the load balancing process.
//     *
//     * @param consumer A consumer that accepts {@code LoadBalancerLifecycle} instances for processing.
//     */
//    void lifecycles(Consumer<LoadBalancerLifecycle> consumer);
//}
//
