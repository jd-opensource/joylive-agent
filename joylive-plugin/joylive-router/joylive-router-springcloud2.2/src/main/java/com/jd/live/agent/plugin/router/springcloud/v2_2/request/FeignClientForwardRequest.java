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

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpForwardRequest;
import com.jd.live.agent.governance.request.HostTransformer;
import feign.Request;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.getFirst;
import static com.jd.live.agent.core.util.CollectionUtils.set;

/**
 * Feign client forward request implementation for multi-active or lane-based domain conversion.
 */
public class FeignClientForwardRequest extends AbstractHttpForwardRequest<Request> implements FeignOutboundRequest {

    public FeignClientForwardRequest(Request request, URI uri, HostTransformer hostTransformer) {
        super(request, uri, hostTransformer);
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.ofNullable(request.httpMethod().name());
    }

    @Override
    public String getHeader(String key) {
        return getFirst(request.headers(), key);
    }

    @Override
    public void setHeader(String key, String value) {
        set(request.headers(), key, value);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return MultiLinkedMap.caseInsensitive(request.headers(), true);
    }

}
