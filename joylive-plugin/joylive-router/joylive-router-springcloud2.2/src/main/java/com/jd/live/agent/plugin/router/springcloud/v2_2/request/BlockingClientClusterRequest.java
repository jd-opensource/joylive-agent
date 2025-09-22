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
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_2.util.CloudUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class BlockingClientClusterRequest extends AbstractHttpOutboundRequest<BlockingClientHttpRequest> {

    private final String service;

    private final Registry registry;

    public BlockingClientClusterRequest(BlockingClientHttpRequest request, String service, Registry registry) {
        super(request);
        this.service = service;
        this.registry = registry;
        this.uri = request.getURI();
    }

    @Override
    public String getService() {
        return service == null || service.isEmpty() ? super.getService() : service;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.ofNullable(request.getMethodValue());
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : request.getHeaders().getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            CloudUtils.writable(request.getHeaders()).set(key, value);
        }
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return request.getHeaders();
    }

    public CompletionStage<List<ServiceEndpoint>> getInstances() {
        return registry.getEndpoints(service);
    }
}
