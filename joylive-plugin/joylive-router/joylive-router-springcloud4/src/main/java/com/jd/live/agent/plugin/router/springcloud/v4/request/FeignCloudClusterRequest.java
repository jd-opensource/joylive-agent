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

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.context.FeignClusterContext;
import feign.Request;
import feign.Response;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMapAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.getFirst;
import static com.jd.live.agent.core.util.CollectionUtils.modifiedMap;
import static com.jd.live.agent.core.util.map.MultiLinkedMap.caseInsensitive;
import static com.jd.live.agent.plugin.router.springcloud.v4.util.UriUtils.newURI;

/**
 * Represents an outbound request made using Feign, extending the capabilities of {@link AbstractCloudClusterRequest}
 * to handle specifics of Feign requests such as options and cookie parsing.
 *
 * @since 1.0.0
 */
public class FeignCloudClusterRequest extends AbstractCloudClusterRequest<Request, FeignClusterContext> {

    private final Request.Options options;

    private final LazyObject<Map<String, Collection<String>>> cache = new LazyObject<>(() -> modifiedMap(request.headers()));

    public FeignCloudClusterRequest(Request request, Request.Options options, FeignClusterContext context) {
        super(request, URI.create(request.url()), context);
        this.options = options;
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
    protected RequestData buildRequestData() {
        // cookie is used only in RequestBasedStickySessionServiceInstanceListSupplier
        // it's disabled by live interceptor
        // so we can use null value to improve performance.
        return new RequestData(
                org.springframework.http.HttpMethod.valueOf(request.httpMethod().name()), getURI(),
                new HttpHeaders(new MultiValueMapAdapter<>(getHeaders())), null, null);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return caseInsensitive(request.headers(), true);
    }

    /**
     * Executes the HTTP request for a specific service instance.
     *
     * @param endpoint the {@link ServiceEndpoint} to which the request is directed
     * @return the {@link Response} containing the response data
     * @throws IOException if an I/O error occurs during the request execution
     */
    public Response execute(ServiceEndpoint endpoint) throws IOException {
        String url = newURI(endpoint, uri).toString();
        // TODO sticky session
        Request req = Request.create(request.httpMethod(), url, request.headers(),
                request.body(), request.charset(), request.requestTemplate());
        return context.getDelegate().execute(req, options);
    }

}
