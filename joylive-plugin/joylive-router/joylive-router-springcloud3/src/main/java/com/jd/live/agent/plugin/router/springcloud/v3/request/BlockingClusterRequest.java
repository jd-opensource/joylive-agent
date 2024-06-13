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
package com.jd.live.agent.plugin.router.springcloud.v3.request;

import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.util.MultiValueMapAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a blocking request in a routing context, extending the capabilities of {@link AbstractClusterRequest}
 * to handle HTTP requests in environments where reactive programming models are not used.
 *
 * @see AbstractClusterRequest for the base functionality
 */
public class BlockingClusterRequest extends AbstractClusterRequest<HttpRequest> {

    private static final String COOKIE_HEADER = "Cookie";

    /**
     * The body of the HTTP request.
     */
    private final byte[] body;

    /**
     * The execution context for the client HTTP request, allowing for further processing or manipulation.
     */
    private final ClientHttpRequestExecution execution;

    /**
     * Constructs a new {@code BlockingRouteRequest} with the specified request details and load balancing context.
     *
     * @param request             The original HTTP request.
     * @param loadBalancerFactory The factory to obtain a load balancer instance for routing decisions.
     * @param body                The body of the request as a byte array.
     * @param execution           The execution context for processing the request.
     */
    public BlockingClusterRequest(HttpRequest request,
                                  ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
                                  byte[] body,
                                  ClientHttpRequestExecution execution) {
        super(request, loadBalancerFactory);
        this.uri = request.getURI();
        this.queries = new UnsafeLazyObject<>(() -> parseQuery(request.getURI().getQuery()));
        this.headers = new UnsafeLazyObject<>(request::getHeaders);
        this.cookies = new UnsafeLazyObject<>(this::parseCookie);
        this.body = body;
        this.execution = execution;
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.getMethodValue());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getCookie(String key) {
        if (key == null) {
            return null;
        }
        List<String> values = cookies.get().get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    protected RequestData buildRequestData() {
        return new RequestData(request.getMethod(), request.getURI(), request.getHeaders(),
                new MultiValueMapAdapter<>(cookies.get()), new HashMap<>());
    }

    public byte[] getBody() {
        return body;
    }

    public ClientHttpRequestExecution getExecution() {
        return execution;
    }

    /**
     * Parses the cookies from the HTTP request.
     *
     * @return A map of cookie names to their respective list of values.
     */
    protected Map<String, List<String>> parseCookie() {
        Map<String, List<String>> result = new HashMap<>();
        List<String> cookies = request.getHeaders().get(COOKIE_HEADER);
        if (cookies != null) {
            cookies.forEach(cookie -> {
                String[] keyValue = StringUtils.split(cookie, '=');
                if (keyValue.length < 2) {
                    return;
                }
                result.computeIfAbsent(keyValue[0], key -> new ArrayList<>()).add(keyValue[1]);
            });
        }
        return result;
    }
}
