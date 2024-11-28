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
package com.jd.live.agent.plugin.router.springcloud.v4.response;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.IOUtils;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * BlockingRouteResponse
 *
 * @since 1.0.0
 */
public class BlockingClusterResponse extends AbstractHttpOutboundResponse<ClientHttpResponse> {

    private static final Logger logger = LoggerFactory.getLogger(BlockingClusterResponse.class);

    private byte[] body;

    public BlockingClusterResponse(ClientHttpResponse response) {
        super(response);
        this.headers = new UnsafeLazyObject<>(response::getHeaders);
        this.cookies = new UnsafeLazyObject<>(() -> HttpUtils.parseCookie(response.getHeaders().get(HttpHeaders.COOKIE)));
    }

    public BlockingClusterResponse(ServiceError error, ErrorPredicate predicate) {
        super(error, predicate);
    }

    @Override
    public String getCode() {
        try {
            HttpStatusCode status = response == null ? null : response.getStatusCode();
            return status == null ? null : String.valueOf(status.value());
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
                    InputStream in = response.getBody();
                    body = IOUtils.read(in);
                    response = new ClientHttpResponseAdapter(response, body);
                    Close.instance().close(in);
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                    body = new byte[0];
                }
            }
        }
        return body;
    }

    private static class ClientHttpResponseAdapter implements ClientHttpResponse {

        private final ClientHttpResponse delegate;

        private final byte[] body;

        ClientHttpResponseAdapter(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = body;
        }

        @NonNull
        @Override
        public HttpStatusCode getStatusCode() throws IOException {
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
