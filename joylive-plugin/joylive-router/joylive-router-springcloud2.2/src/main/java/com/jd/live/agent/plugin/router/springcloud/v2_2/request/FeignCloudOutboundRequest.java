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
package com.jd.live.agent.plugin.router.springcloud.v2_2.request;

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import feign.Request;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.getFirst;
import static com.jd.live.agent.core.util.CollectionUtils.modifiedMap;
import static com.jd.live.agent.core.util.map.MultiLinkedMap.caseInsensitive;

/**
 * FeignOutboundRequest
 *
 * @since 1.5.0
 */
public class FeignCloudOutboundRequest extends AbstractHttpOutboundRequest<Request> {

    private final String serviceId;

    private final LazyObject<Map<String, Collection<String>>> cache = new LazyObject<>(() -> modifiedMap(request.headers()));

    public FeignCloudOutboundRequest(Request request, String serviceId) {
        super(request);
        this.serviceId = serviceId;
        this.uri = URI.create(request.url());
    }

    @Override
    public String getService() {
        return serviceId == null || serviceId.isEmpty() ? super.getService() : serviceId;
    }

    @Override
    public HttpMethod getHttpMethod() {
        Request.HttpMethod method = request.httpMethod();
        return method == null ? null : HttpMethod.ofNullable(method.name());
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : getFirst(request.headers().get(key));
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            cache.get().computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return caseInsensitive(request.headers(), true);
    }

}
