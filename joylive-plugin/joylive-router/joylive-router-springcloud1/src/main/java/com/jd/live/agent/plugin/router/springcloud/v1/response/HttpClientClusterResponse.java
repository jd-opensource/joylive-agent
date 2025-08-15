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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.core.util.map.MultiMap;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * HttpClientClusterResponse
 */
public class HttpClientClusterResponse extends AbstractHttpOutboundResponse<HttpResponse> implements SpringClusterResponse {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientClusterResponse.class);

    private byte[] body;

    public HttpClientClusterResponse(HttpResponse response) {
        super(response);
    }

    public HttpClientClusterResponse(ServiceError error, ErrorPredicate predicate) {
        super(error, predicate);
    }

    @Override
    public String getCode() {
        return Integer.toString(response == null ? INTERNAL_SERVER_ERROR.value() : response.getStatusLine().getStatusCode());
    }

    @Override
    public Object getResult() {
        if (body == null) {
            HttpEntity entity = response == null ? null : response.getEntity();
            if (entity == null) {
                body = new byte[0];
            } else {
                try {
                    body = EntityUtils.toByteArray(entity);
                    response.setEntity(new ByteArrayEntity(body));
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                    body = new byte[0];
                }
            }
        }
        return body;
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return HttpUtils.parseCookie(getHeaders(HttpHeaders.COOKIE));
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        if (response == null) {
            return null;
        }
        Header[] headers = response.getAllHeaders();
        if (headers == null) {
            return null;
        }
        MultiMap<String, String> result = MultiLinkedMap.caseInsensitive(headers.length);
        for (Header header : headers) {
            result.add(header.getName(), header.getValue());
        }
        return result;
    }

    /**
     * Creates degraded response using fallback configuration
     *
     * @param request       Http client request
     * @param degradeConfig Fallback settings including response body/headers
     * @return Preconfigured fallback response wrapper
     */
    public static CloseableHttpResponse create(HttpRequest request, DegradeConfig degradeConfig) {
        LiveHttpResponse response = new LiveHttpResponse(HttpVersion.HTTP_1_1, degradeConfig.getResponseCode(), "OK");
        byte[] data = degradeConfig.getResponseBytes();
        degradeConfig.foreach(response::addHeader);
        response.addHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length));
        response.addHeader(HttpHeaders.CONTENT_TYPE, degradeConfig.getContentType());
        response.setEntity(new ByteArrayEntity(data));
        return response;
    }

    private static class LiveHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {

        LiveHttpResponse(StatusLine statusline, ReasonPhraseCatalog catalog, Locale locale) {
            super(statusline, catalog, locale);
        }

        LiveHttpResponse(StatusLine statusline) {
            super(statusline);
        }

        LiveHttpResponse(ProtocolVersion ver, int code, String reason) {
            super(ver, code, reason);
        }

        @Override
        public void close() {
            // do nothing
        }
    }
}
