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

import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * A {@link ClientHttpResponse} implementation that uses {@link DegradeConfig} for response configuration.
 */
public class DegradeHttpResponse implements ClientHttpResponse {
    private final DegradeConfig degradeConfig;
    private final HttpOutboundRequest request;
    private final int length;
    private final InputStream bodyStream;

    public DegradeHttpResponse(DegradeConfig degradeConfig, HttpOutboundRequest request) {
        this.degradeConfig = degradeConfig;
        this.request = request;
        this.length = degradeConfig.getBodyLength();
        this.bodyStream = new ByteArrayInputStream(degradeConfig.getResponseBytes());
    }

    @NonNull
    @Override
    public HttpStatus getStatusCode() throws IOException {
        try {
            return HttpStatus.valueOf(degradeConfig.getResponseCode());
        } catch (Throwable e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    @NonNull
    @Override
    public String getStatusText() throws IOException {
        return "";
    }

    @Override
    public void close() {

    }

    @NonNull
    @Override
    public InputStream getBody() throws IOException {
        return bodyStream;
    }

    @NonNull
    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Map<String, List<String>> requestHeaders = request.getHeaders();
        if (requestHeaders != null) {
            headers.putAll(requestHeaders);
        }
        degradeConfig.foreach(headers::add);
        headers.set(HttpHeaders.CONTENT_TYPE, degradeConfig.getContentType());
        headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
        return headers;
    }
}
