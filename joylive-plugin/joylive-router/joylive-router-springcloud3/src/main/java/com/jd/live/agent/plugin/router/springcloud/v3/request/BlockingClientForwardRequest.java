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
package com.jd.live.agent.plugin.router.springcloud.v3.request;

import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpForwardRequest;
import com.jd.live.agent.governance.request.HostTransformer;
import com.jd.live.agent.governance.request.HttpRequest;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.http.HttpUtils.newURI;

/**
 * BlockingForwardRequest
 */
public class BlockingClientForwardRequest extends AbstractHttpForwardRequest<BlockingClientHttpRequest> implements HttpRequest.HttpForwardRequest {

    private final HttpHeaders writeableHeaders;

    public BlockingClientForwardRequest(BlockingClientHttpRequest request, URI uri, HostTransformer hostTransformer) {
        super(request, uri, hostTransformer);
        this.writeableHeaders = HttpHeaders.writableHttpHeaders(request.getHeaders());
    }

    @Override
    public String getService() {
        return null;
    }

    @Override
    public com.jd.live.agent.core.util.http.HttpMethod getHttpMethod() {
        return com.jd.live.agent.core.util.http.HttpMethod.ofNullable(request.getMethodValue());
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : writeableHeaders.getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            writeableHeaders.set(key, value);
        }
    }

    @Override
    public void forward(String host) {
        uri = newURI(uri, host);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return writeableHeaders;
    }

}
