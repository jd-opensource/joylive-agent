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

import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import feign.Request;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.modifiedMap;

/**
 * FeignWebOutboundRequest
 */
public class FeignWebOutboundRequest extends AbstractHttpOutboundRequest<Request> implements FeignOutboundRequest {

    private final String service;

    private CacheObject<Map<String, Collection<String>>> writeableHeaders;

    public FeignWebOutboundRequest(Request request, String service, URI uri) {
        super(request);
        this.service = service;
        this.uri = uri;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.httpMethod().name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getHeader(String key) {
        Collection<String> values = request.headers().get(key);
        return values == null || values.isEmpty() ? null : values.iterator().next();
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            getWriteableHeaders().computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return MultiLinkedMap.caseInsensitive(request.headers(), true);
    }

    protected Map<String, Collection<String>> getWriteableHeaders() {
        if (writeableHeaders == null) {
            writeableHeaders = new CacheObject<>(modifiedMap(request.headers()));
        }
        return writeableHeaders.get();
    }

}
