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

import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v2.cluster.context.CloudClusterContext;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an outbound HTTP request in a reactive microservices architecture,
 * extending the capabilities of an abstract HTTP outbound request model to include
 * client-specific functionalities. This class encapsulates features such as load balancing,
 * service instance discovery, and lifecycle management, making it suitable for handling
 * dynamic client requests in a distributed system.
 */
public abstract class AbstractCloudClusterRequest<T, C extends CloudClusterContext> extends AbstractHttpOutboundRequest<T> implements SpringClusterRequest {

    protected final C context;

    /**
     * A lazy-initialized {@code ServiceInstanceListSupplier} object, responsible for providing
     * a list of available service instances for load balancing.
     */
    protected CacheObject<ServiceInstanceListSupplier> instanceSupplier;

    public AbstractCloudClusterRequest(T request, URI uri, C context) {
        super(request);
        this.uri = uri;
        this.context = context;
    }

    @Override
    public String getCookie(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        } else if (request instanceof ServerHttpRequest) {
            ServerHttpRequest httpRequest = (ServerHttpRequest) request;
            HttpCookie cookie = httpRequest.getCookies().getFirst(key);
            return cookie == null ? null : cookie.getValue();
        } else {
            return super.getCookie(key);
        }
    }

    @Override
    public RetryPolicy getDefaultRetryPolicy() {
        RetryPolicy retryPolicy = context.getDefaultRetryPolicy();
        return retryPolicy != null && retryPolicy.containsMethod(getMethod()) ? retryPolicy : null;
    }

    @Override
    public Mono<List<ServiceInstance>> getInstances() {
        ServiceInstanceListSupplier supplier = getInstanceSupplier();
        if (supplier == null) {
            return Mono.just(new ArrayList<>());
        } else {
            return supplier.get().next();
        }
    }

    /**
     * Returns a supplier of service instance lists.
     *
     * @return a supplier of service instance lists
     */
    protected ServiceInstanceListSupplier getInstanceSupplier() {
        if (instanceSupplier == null) {
            instanceSupplier = new CacheObject<>(context.getServiceInstanceListSupplier(getService()));
        }
        return instanceSupplier.get();
    }
}
