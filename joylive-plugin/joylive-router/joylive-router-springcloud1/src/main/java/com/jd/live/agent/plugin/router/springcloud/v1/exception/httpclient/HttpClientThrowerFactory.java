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
package com.jd.live.agent.plugin.router.springcloud.v1.exception.httpclient;

import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.exception.ThrowerFactory;
import org.apache.http.client.ClientProtocolException;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/**
 * A concrete implementation of {@link ThrowerFactory} that creates exceptions specifically
 * for handling HTTP status codes, typically used in Spring WebFlux applications.
 */
public class HttpClientThrowerFactory<R extends HttpOutboundRequest> implements ThrowerFactory<IOException, R> {

    @Override
    public IOException createException(R request, HttpStatus status, String message, Throwable throwable) {
        if (throwable instanceof IOException) {
            return (IOException) throwable;
        }
        return new ClientProtocolException(message, throwable);
    }
}
