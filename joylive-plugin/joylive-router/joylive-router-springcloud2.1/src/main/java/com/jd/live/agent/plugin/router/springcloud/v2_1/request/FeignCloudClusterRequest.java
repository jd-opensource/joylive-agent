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
package com.jd.live.agent.plugin.router.springcloud.v2_1.request;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.context.FeignClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v2_1.instance.EndpointInstance;
import feign.Request;
import feign.Response;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.*;
import static com.jd.live.agent.core.util.map.MultiLinkedMap.caseInsensitive;
import static com.jd.live.agent.plugin.router.springcloud.v2_1.util.UriUtils.newURI;

/**
 * Represents an outbound request made using Feign, extending the capabilities of {@link AbstractCloudClusterRequest}
 * to handle specifics of Feign requests such as options and cookie parsing.
 *
 * @since 1.0.0
 */
public class FeignCloudClusterRequest extends AbstractCloudClusterRequest<Request, FeignClusterContext> {

    private final Request.Options options;

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
        return getFirst(request.headers(), key);
    }

    @Override
    public void setHeader(String key, String value) {
        set(modifiedMap(request.headers()), key, value);
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
    @SuppressWarnings("deprecation")
    public Response execute(ServiceEndpoint endpoint) throws IOException {
        String url = newURI(EndpointInstance.convert(endpoint), uri).toString();
        Request req = Request.create(request.httpMethod(), url, request.headers(), request.body(), request.charset());
        return context.getDelegate().execute(req, options);
    }

}
