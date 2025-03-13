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
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.util.List;
import java.util.Map;

/**
 * ReactiveOutboundRequest
 */
public class ReactiveCloudOutboundRequest extends AbstractHttpOutboundRequest<ClientRequest> {

    private final String serviceId;

    private final HttpHeaders writeableHeaders;

    public ReactiveCloudOutboundRequest(ClientRequest request, String serviceId) {
        super(request);
        this.serviceId = serviceId;
        this.uri = request.url();
        this.writeableHeaders = HttpHeaders.writableHttpHeaders(request.headers());
    }

    @Override
    public String getService() {
        return serviceId;
    }

    @Override
    public HttpMethod getHttpMethod() {
        org.springframework.http.HttpMethod method = request.method();
        try {
            return method == null ? null : HttpMethod.valueOf(method.name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : writeableHeaders.getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            writeableHeaders.set(key, value);
        }
    }

    @Override
    public String getCookie(String key) {
        return request.cookies().getFirst(key);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return writeableHeaders;
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return request.cookies();
    }
}
