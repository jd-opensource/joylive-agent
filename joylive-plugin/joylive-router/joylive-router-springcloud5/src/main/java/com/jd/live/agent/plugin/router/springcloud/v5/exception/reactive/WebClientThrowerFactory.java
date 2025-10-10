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
package com.jd.live.agent.plugin.router.springcloud.v5.exception.reactive;

import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v5.exception.ThrowerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;

/**
 * A concrete implementation of {@link ThrowerFactory} that creates exceptions specifically
 */
public class WebClientThrowerFactory<R extends HttpOutboundRequest> implements ThrowerFactory<WebClientException, R> {

    @Override
    public WebClientException createException(R request, HttpStatus status, String message, Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            return (WebClientResponseException) throwable;
        }
        return new WebClientResponseException(status.value(), message, new HttpHeaders(), null, StandardCharsets.UTF_8);
    }
}
