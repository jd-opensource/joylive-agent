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
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import feign.Request;
import org.springframework.http.HttpHeaders;

import java.net.URI;

/**
 * FeignOutboundRequest
 *
 * @since 1.5.0
 */
public class FeignOutboundRequest extends AbstractHttpOutboundRequest<Request> {

    private final String serviceId;

    public FeignOutboundRequest(Request request, String serviceId) {
        super(request);
        this.serviceId = serviceId;
        this.uri = URI.create(request.url());
        this.queries = new UnsafeLazyObject<>(() -> HttpUtils.parseQuery(uri.getRawQuery()));
        this.headers = new UnsafeLazyObject<>(() -> new MultiLinkedMap<>(request.headers(), CaseInsensitiveLinkedMap::new));
        this.cookies = new UnsafeLazyObject<>(() -> HttpUtils.parseCookie(request.headers().get(HttpHeaders.COOKIE)));
    }

    @Override
    public String getService() {
        return serviceId == null || serviceId.isEmpty() ? super.getService() : serviceId;
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
