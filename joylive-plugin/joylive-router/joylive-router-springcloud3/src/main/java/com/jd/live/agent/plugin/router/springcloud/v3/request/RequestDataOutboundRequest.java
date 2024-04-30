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

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import com.jd.live.agent.governance.request.Cookie;
import com.jd.live.agent.governance.request.HttpMethod;
import org.springframework.cloud.client.loadbalancer.RequestData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RequestDataOutboundRequest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class RequestDataOutboundRequest extends AbstractHttpOutboundRequest<RequestData> {

    private final String serviceId;

    public RequestDataOutboundRequest(RequestData request, String serviceId) {
        super(request);
        this.serviceId = serviceId;
        this.uri = request.getUrl();
        this.headers = new LazyObject<>(request.getHeaders());
        this.queries = new LazyObject<>(() -> parseQuery(request.getUrl().getQuery()));
        this.cookies = new LazyObject<>(() -> parseCookie(request));
    }

    @Override
    public String getService() {
        return serviceId == null || serviceId.isEmpty() ? super.getService() : serviceId;
    }

    @Override
    public HttpMethod getHttpMethod() {
        org.springframework.http.HttpMethod method = request.getHttpMethod();
        try {
            return method == null ? null : HttpMethod.valueOf(method.name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getCookie(String key) {
        String result = null;
        if (request != null) {
            result = request.getCookies().getFirst(key);
        }
        return result;
    }

    protected Map<String, List<Cookie>> parseCookie(RequestData request) {
        Map<String, List<Cookie>> result = new HashMap<>();
        request.getCookies().forEach((s, strings) -> result.put(s,
                strings.stream().map(c -> new Cookie(s, c)).collect(Collectors.toList())));
        return result;
    }
}
