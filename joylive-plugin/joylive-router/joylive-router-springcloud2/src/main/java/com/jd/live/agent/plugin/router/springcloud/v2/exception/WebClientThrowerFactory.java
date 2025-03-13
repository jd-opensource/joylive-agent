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
package com.jd.live.agent.plugin.router.springcloud.v2.exception;

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;

/**
 * A concrete implementation of {@link ThrowerFactory} that creates exceptions specifically
 */
public class WebClientThrowerFactory implements ThrowerFactory {

    public static final ThrowerFactory INSTANCE = new WebClientThrowerFactory();

    @Override
    public NestedRuntimeException createException(HttpStatus status, String message, Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            return (WebClientResponseException) throwable;
        }
        return new WebClientResponseException(status.value(), message, new HttpHeaders(), null, StandardCharsets.UTF_8);
    }
}
