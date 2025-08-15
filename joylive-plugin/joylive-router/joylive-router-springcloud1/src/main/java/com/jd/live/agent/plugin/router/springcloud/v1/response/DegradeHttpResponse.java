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
package com.jd.live.agent.plugin.router.springcloud.v1.response;

import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.springcloud.v1.request.BlockingCloudClusterRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

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
    private final BlockingCloudClusterRequest httpRequest;
    private final int length;
    private final InputStream bodyStream;

    public DegradeHttpResponse(DegradeConfig degradeConfig, BlockingCloudClusterRequest httpRequest) {
        this.degradeConfig = degradeConfig;
        this.httpRequest = httpRequest;
        this.length = degradeConfig.getBodyLength();
        this.bodyStream = new ByteArrayInputStream(degradeConfig.getResponseBytes());
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
        try {
            return HttpStatus.valueOf(degradeConfig.getResponseCode());
        } catch (Throwable e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return degradeConfig.getResponseCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return "";
    }

    @Override
    public void close() {

    }

    @Override
    public InputStream getBody() throws IOException {
        return bodyStream;
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Map<String, List<String>> requestHeaders = httpRequest.getHeaders();
        if (requestHeaders != null) {
            headers.putAll(requestHeaders);
        }
        degradeConfig.foreach(headers::add);
        headers.set(HttpHeaders.CONTENT_TYPE, degradeConfig.getContentType());
        headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
        return headers;
    }
}
