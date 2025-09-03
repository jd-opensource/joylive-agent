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
package com.jd.live.agent.plugin.router.springcloud.v1.request;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

import java.util.List;
import java.util.Map;

/**
 * ReactiveOutboundRequest
 *
 * @since 1.7.0
 */
public class BlockingCloudOutboundRequest extends AbstractHttpOutboundRequest<HttpRequest> {

    private final String serviceId;

    private final HttpHeaders headers;

    public BlockingCloudOutboundRequest(HttpRequest request, String serviceId) {
        super(request);
        this.serviceId = serviceId;
        this.uri = request.getURI();
        this.headers = request.getHeaders();
    }

    @Override
    public String getService() {
        return serviceId == null || serviceId.isEmpty() ? super.getService() : serviceId;
    }

    @Override
    public HttpMethod getHttpMethod() {
        org.springframework.http.HttpMethod method = request.getMethod();
        return method == null ? null : HttpMethod.ofNullable(method.name());
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : request.getHeaders().getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            try {
                headers.set(key, value);
            } catch (UnsupportedOperationException ignored) {
                // readonly for commited message
            }
        }
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return headers;
    }
}
