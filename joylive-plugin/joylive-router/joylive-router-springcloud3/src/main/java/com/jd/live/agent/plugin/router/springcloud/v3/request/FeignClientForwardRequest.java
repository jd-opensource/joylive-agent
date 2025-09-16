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

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import com.jd.live.agent.governance.request.HttpRequest.HttpForwardRequest;
import feign.Request;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.http.HttpUtils.newURI;

/**
 * FeignForwardRequest
 */
public class FeignClientForwardRequest extends AbstractHttpOutboundRequest<Request> implements HttpForwardRequest {

    public FeignClientForwardRequest(Request request, URI uri) {
        super(request);
        this.uri = uri;
    }

    @Override
    public String getService() {
        return null;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.ofNullable(request.httpMethod().name());
    }

    @Override
    public String getHeader(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Collection<String> values = request.headers().get(key);
        if (values == null || values.isEmpty()) {
            return null;
        } else if (values instanceof List) {
            return ((List<String>) values).get(0);
        }
        return values.iterator().next();
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            List<String> values = new ArrayList<>();
            values.add(value);
            request.headers().put(key, values);
        }
    }

    @Override
    public void forward(String host) {
        uri = newURI(uri, host);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return MultiLinkedMap.caseInsensitive(request.headers(), true);
    }

}
