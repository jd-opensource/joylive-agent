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

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Builder interface for creating blocking HTTP client requests.
 */
public interface BlockingClientHttpRequestBuilder {

    /**
     * Gets the target URI.
     *
     * @return the URI
     */
    URI getUri();

    /**
     * Gets the HTTP method.
     *
     * @return the HTTP method
     */
    HttpMethod getMethod();

    List<ClientHttpRequestInterceptor> getInterceptors();

    /**
     * Initializes the client HTTP request.
     *
     * @param request the request to initialize
     */
    void initialize(ClientHttpRequest request);

    /**
     * Creates a client HTTP request for the given URI and method.
     *
     * @param uri    the target URI
     * @param method the HTTP method
     * @return the created request
     * @throws IOException if request creation fails
     */
    ClientHttpRequest create(URI uri, HttpMethod method) throws IOException;

}
