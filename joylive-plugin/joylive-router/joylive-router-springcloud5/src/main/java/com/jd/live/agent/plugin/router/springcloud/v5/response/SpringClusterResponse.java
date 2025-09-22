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
package com.jd.live.agent.plugin.router.springcloud.v5.response;

import com.jd.live.agent.governance.response.HttpResponse.HttpOutboundResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * An interface that extends {@link HttpOutboundResponse} to provide methods for accessing
 * HTTP response-related information, such as status code, headers, and cookies.
 */
public interface SpringClusterResponse extends HttpOutboundResponse {

    /**
     * Retrieves the HTTP status code of the response.
     *
     * @return the {@link HttpStatus} of the response
     */
    int getStatusCode();

    /**
     * Retrieves the HTTP status code of the response.
     *
     * @return the {@link HttpStatus} representing the HTTP status code
     */
    HttpStatusCode getHttpStatus();

    /**
     * Retrieves the HTTP headers of the response.
     *
     * @return the {@link HttpHeaders} of the response
     */
    HttpHeaders getHttpHeaders();

}

