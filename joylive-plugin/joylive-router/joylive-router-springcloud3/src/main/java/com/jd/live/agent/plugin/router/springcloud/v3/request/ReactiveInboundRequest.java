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
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpInboundRequest;
import com.jd.live.agent.governance.request.HttpMethod;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReactiveInboundRequest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ReactiveInboundRequest extends AbstractHttpInboundRequest<ServerHttpRequest> {

    public ReactiveInboundRequest(ServerHttpRequest request) {
        super(request);
        this.uri = request.getURI();
        this.headers = new LazyObject<>(request.getHeaders());
        this.queries = new LazyObject<>(request.getQueryParams());
        this.cookies = new LazyObject<>(() -> parseCookie(request));
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
        if (key != null) {
            HttpCookie cookie = request.getCookies().getFirst(key);
            result = cookie == null ? null : cookie.getValue();
        }
        return result;
    }

    protected Map<String, List<String>> parseCookie(ServerHttpRequest request) {
        Map<String, List<String>> result = new HashMap<>();
        request.getCookies().forEach((n, v) -> result.put(n,
                v.stream().map(HttpCookie::getValue).collect(Collectors.toList())));
        return result;
    }
}
