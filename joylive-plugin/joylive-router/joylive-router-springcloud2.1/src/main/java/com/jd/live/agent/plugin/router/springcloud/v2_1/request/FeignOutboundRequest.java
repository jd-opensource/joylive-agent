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

import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import feign.Request;

import java.net.URI;

/**
 * An interface representing an outbound HTTP request in the context of Feign.
 * Extends {@link HttpOutboundRequest} to provide additional functionality specific to Feign requests.
 */
public interface FeignOutboundRequest extends HttpOutboundRequest {

    /**
     * Retrieves the underlying {@link Request} object associated with this outbound request.
     *
     * @return the {@link Request} object representing the Feign request
     */
    Request getRequest();

    /**
     * Creates a new {@link Request} object based on the provided URI, original request, and endpoint.
     * This method constructs a new URI using the provided endpoint's host and port, and then creates a new request
     * with the same HTTP method, headers, and body as the original request.
     *
     * @param uri      the original URI of the request
     * @param request  the original {@link Request} object
     * @param endpoint the {@link Endpoint} object containing the host and port to use for the new URI
     * @return a new {@link Request} object with the updated URI and the same properties as the original request
     */
    static Request createRequest(URI uri, Request request, Endpoint endpoint) {
        URI newUri = HttpUtils.newURI(uri, endpoint.getHost(), endpoint.getPort());
        return Request.create(
                request.httpMethod(),
                newUri.toString(),
                request.headers(),
                Request.Body.create(request.body()),
                request.requestTemplate());
    }

}

