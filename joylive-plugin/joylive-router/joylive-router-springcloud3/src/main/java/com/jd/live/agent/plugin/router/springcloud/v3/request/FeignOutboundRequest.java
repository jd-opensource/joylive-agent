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
import com.jd.live.agent.governance.request.AbstractHttpRequest;
import com.jd.live.agent.governance.request.Cookie;
import com.jd.live.agent.governance.request.HttpMethod;
import feign.Request;

import java.net.HttpCookie;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FeignOutboundRequest
 *
 * @since 1.0.0
 */
public class FeignOutboundRequest extends AbstractHttpRequest.AbstractHttpOutboundRequest<Request> {

    private static final String COOKIE_HEADER = "Cookie";

    private final String serviceId;

    public FeignOutboundRequest(Request request, String serviceId) {
        super(request);
        this.serviceId = serviceId;
        this.uri = URI.create(request.url());
        this.queries = new LazyObject<>(() -> parseQuery(request.requestTemplate().queryLine()));
        this.headers = new LazyObject<>(convertToListMap(request.headers()));
        this.cookies = new LazyObject<>(() -> parseCookie(request.headers().get(COOKIE_HEADER)));
    }

    public static <K, V> Map<K, List<V>> convertToListMap(Map<K, ? extends Collection<V>> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(e.getValue())
                ));
    }

    protected Map<String, List<Cookie>> parseCookie(Collection<String> headers) {
        Map<String, List<Cookie>> result = new HashMap<>();
        if (headers == null || headers.isEmpty()) {
            return result;
        }
        for (String header : headers) {
            if (header != null && !header.isEmpty()) {
                String[] values = header.split(";");
                List<HttpCookie> cookies;
                for (String cookie : values) {
                    cookies = HttpCookie.parse(cookie.trim());
                    cookies.forEach(c -> result.computeIfAbsent(c.getName(), k -> new ArrayList<>()).add(new Cookie(c.getName(), c.getValue())));
                }
            }
        }
        return result;
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
}
