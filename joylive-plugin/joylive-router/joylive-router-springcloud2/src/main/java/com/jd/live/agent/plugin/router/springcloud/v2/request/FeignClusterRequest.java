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

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.map.CaseInsensitiveLinkedMap;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import feign.Request;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;

import java.net.URI;

/**
 * Represents an outbound request made using Feign, extending the capabilities of {@link AbstractClusterRequest}
 * to handle specifics of Feign requests such as options and cookie parsing.
 * <p>
 * This class encapsulates the details of a Feign request, including HTTP method, URI, headers, and cookies,
 * and provides utilities for parsing these elements from the Feign {@link Request}. It also integrates with
 * Spring's {@link LoadBalancerClientFactory} for load balancing capabilities.
 *
 * @since 1.5.0
 */
public class FeignClusterRequest extends AbstractClusterRequest<Request> {

    private final Request.Options options;

    /**
     * Constructs a new {@code FeignOutboundRequest} with the specified Feign request, load balancer client factory,
     * and request options.
     *
     * @param request             the Feign request
     * @param loadBalancerFactory the factory to create a load balancer client
     * @param options             the options for the Feign request, such as timeouts
     */
    public FeignClusterRequest(Request request,
                               ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
                               Request.Options options) {
        super(request, loadBalancerFactory);
        this.options = options;
        this.uri = URI.create(request.url());
        this.queries = new UnsafeLazyObject<>(() -> HttpUtils.parseQuery(request.requestTemplate().queryLine()));
        this.headers = new UnsafeLazyObject<>(() -> new MultiLinkedMap<>(request.headers(), CaseInsensitiveLinkedMap::new));
        this.cookies = new UnsafeLazyObject<>(() -> HttpUtils.parseCookie(request.headers().get(HttpHeaders.COOKIE)));
    }

    @Override
    public HttpMethod getHttpMethod() {
        Request.HttpMethod method = request.httpMethod();
        try {
            return method == null ? null : HttpMethod.valueOf(method.name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    public Request.Options getOptions() {
        return options;
    }

}
