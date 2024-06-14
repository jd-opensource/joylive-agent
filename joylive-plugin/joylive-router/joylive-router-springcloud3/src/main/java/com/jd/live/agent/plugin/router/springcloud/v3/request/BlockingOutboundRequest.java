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

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * ReactiveOutboundRequest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class BlockingOutboundRequest extends AbstractHttpOutboundRequest<HttpRequest> {

    private final String serviceId;

    public BlockingOutboundRequest(HttpRequest request, String serviceId) {
        super(request);
        this.serviceId = serviceId;
        this.uri = request.getURI();
        this.queries = new UnsafeLazyObject<>(() -> HttpUtils.parseQuery(request.getURI().getQuery()));
        this.headers = new UnsafeLazyObject<>(() -> HttpHeaders.writableHttpHeaders(request.getHeaders()));
        this.cookies = new UnsafeLazyObject<>(() -> request instanceof ServerHttpRequest
                ? HttpUtils.parseCookie(((ServerHttpRequest) request).getCookies(), HttpCookie::getValue)
                : HttpUtils.parseCookie(HttpHeaders.writableHttpHeaders(request.getHeaders()).get(HttpHeaders.COOKIE)));
    }

    @Override
    public String getService() {
        return serviceId == null || serviceId.isEmpty() ? super.getService() : serviceId;
    }

    @Override
    public HttpMethod getHttpMethod() {
        org.springframework.http.HttpMethod method = request.getMethod();
        try {
            return method == null ? null : HttpMethod.valueOf(method.name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getCookie(String key) {
        String result = null;
        if (request instanceof ServerHttpRequest) {
            HttpCookie cookie = ((ServerHttpRequest) request).getCookies().getFirst(key);
            result = cookie == null ? null : cookie.getValue();
        }
        return result;
    }
}
