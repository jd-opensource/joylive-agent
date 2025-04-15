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
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.core.util.map.MultiMap;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.context.HttpClientClusterContext;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.plugin.router.springcloud.v2_1.instance.EndpointInstance.convert;
import static com.jd.live.agent.plugin.router.springcloud.v2_1.util.UriUtils.newURI;

/**
 * Represents an outbound request made using Feign, extending the capabilities of {@link AbstractCloudClusterRequest}
 * to handle specifics of Feign requests such as options and cookie parsing.
 */
public class HttpClientClusterRequest extends AbstractCloudClusterRequest<HttpRequest, HttpClientClusterContext> {

    private final CloseableHttpClient client;

    private final HttpContext httpContext;

    public HttpClientClusterRequest(HttpRequest request,
                                    HttpContext httpContext,
                                    CloseableHttpClient client,
                                    HttpClientClusterContext clusterContext) {
        super(request, URI.create(request.getRequestLine().getUri()), clusterContext);
        this.client = client;
        this.httpContext = httpContext;
    }

    @Override
    public HttpMethod getHttpMethod() {
        String method = request.getRequestLine().getMethod();
        try {
            return method == null ? null : HttpMethod.valueOf(method);
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            request.addHeader(key, value);
        }
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        if (request == null) {
            return null;
        }
        Header[] headers = request.getAllHeaders();
        if (headers == null) {
            return null;
        }
        MultiMap<String, String> result = MultiLinkedMap.caseInsensitive(headers.length);
        for (Header header : headers) {
            result.add(header.getName(), header.getValue());
        }
        return result;
    }

    /**
     * Executes the HTTP request for a specific service instance.
     *
     * @param endpoint the {@link ServiceEndpoint} to which the request is directed
     * @return the {@link HttpResponse} containing the response data
     * @throws IOException if an I/O error occurs during the request execution
     */
    public CloseableHttpResponse execute(ServiceEndpoint endpoint) throws IOException {
        HttpUriRequest newRequest = RequestBuilder.copy(request).setUri(newURI(convert(endpoint), uri)).build();
        return client.execute(newRequest, httpContext);
    }
}
