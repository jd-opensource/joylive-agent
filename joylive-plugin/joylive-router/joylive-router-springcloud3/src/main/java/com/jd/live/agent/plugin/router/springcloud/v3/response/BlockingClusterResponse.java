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
package com.jd.live.agent.plugin.router.springcloud.v3.response;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.IOUtils;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * BlockingRouteResponse
 *
 * @since 1.0.0
 */
public class BlockingClusterResponse extends AbstractHttpOutboundResponse<ClientHttpResponse> implements SpringClusterResponse {

    private static final Logger logger = LoggerFactory.getLogger(BlockingClusterResponse.class);

    private byte[] body;

    public BlockingClusterResponse(ClientHttpResponse response) {
        super(response);
    }

    public BlockingClusterResponse(ServiceError error, ErrorPredicate predicate) {
        super(error, predicate);
    }

    @Override
    public String getCode() {
        try {
            int code = response != null ? response.getRawStatusCode() : INTERNAL_SERVER_ERROR.value();
            return Integer.toString(code);
        } catch (IOException e) {
            return String.valueOf(INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public Object getResult() {
        if (body == null) {
            if (response == null) {
                body = new byte[0];
            } else {
                try {
                    body = IOUtils.read(response.getBody());
                    response.close();
                    response = new ClientHttpResponseAdapter(response, body);
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                    body = new byte[0];
                }
            }
        }
        return body;
    }

    @Override
    public String getHeader(String key) {
        return response == null || key == null ? null : response.getHeaders().getFirst(key);
    }

    @Override
    public List<String> getHeaders(String key) {
        return response == null || key == null ? null : response.getHeaders().get(key);
    }

    @Override
    public int getStatusCode() {
        try {
            return response == null ? INTERNAL_SERVER_ERROR.value() : response.getRawStatusCode();
        } catch (IOException e) {
            return INTERNAL_SERVER_ERROR.value();
        }
    }

    @Override
    public HttpStatus getHttpStatus() {
        try {
            return response == null ? INTERNAL_SERVER_ERROR : response.getStatusCode();
        } catch (IOException e) {
            return INTERNAL_SERVER_ERROR;
        }
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return response == null ? new HttpHeaders() : response.getHeaders();
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return HttpUtils.parseCookie(getHeaders(HttpHeaders.COOKIE));
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return response == null ? null : response.getHeaders();
    }

    private static class ClientHttpResponseAdapter implements ClientHttpResponse {

        private final ClientHttpResponse delegate;

        private final byte[] body;

        ClientHttpResponseAdapter(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = body;
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return delegate.getRawStatusCode();
        }

        @NonNull
        @Override
        public HttpStatus getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @NonNull
        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @NonNull
        @Override
        public InputStream getBody() throws IOException {
            return new ByteArrayInputStream(body);
        }

        @NonNull
        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

    }
}
