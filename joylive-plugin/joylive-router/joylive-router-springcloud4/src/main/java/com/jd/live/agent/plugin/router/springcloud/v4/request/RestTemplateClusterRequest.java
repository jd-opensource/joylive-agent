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
package com.jd.live.agent.plugin.router.springcloud.v4.request;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;

/**
 * RestTemplateOutboundRequest
 */
public class RestTemplateClusterRequest extends AbstractHttpOutboundRequest<RestTemplateHttpRequest> {

    private final String service;
    private final List<? extends Endpoint> endpoints;
    private final HttpHeaders writeableHeaders;

    public RestTemplateClusterRequest(RestTemplateHttpRequest request, String service, List<? extends Endpoint> endpoints) {
        super(request);
        this.service = service;
        this.endpoints = endpoints;
        this.uri = request.getURI();
        this.writeableHeaders = HttpHeaders.writableHttpHeaders(request.getHeaders());
    }

    @Override
    public String getService() {
        return service == null || service.isEmpty() ? super.getService() : service;
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
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : request.getHeaders().getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            writeableHeaders.set(key, value);
        }
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return writeableHeaders;
    }

    public List<? extends Endpoint> getEndpoints() {
        return endpoints;
    }
}
