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
package com.jd.live.agent.plugin.router.springcloud.v2_2.response;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.core.util.IOUtils;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import feign.Request;
import feign.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.InputStream;
import java.util.*;

import static com.jd.live.agent.core.util.map.MultiLinkedMap.caseInsensitive;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

/**
 * FeignOutboundResponse
 *
 * @since 1.0.0
 */
public class FeignClusterResponse extends AbstractHttpOutboundResponse<Response> implements SpringClusterResponse {

    private static final Logger logger = LoggerFactory.getLogger(FeignClusterResponse.class);

    private byte[] body;

    public FeignClusterResponse(Response response) {
        super(response);
    }

    public FeignClusterResponse(ServiceError error, ErrorPredicate predicate) {
        super(error, predicate);
    }

    @Override
    public String getCode() {
        return Integer.toString(response == null ? INTERNAL_SERVER_ERROR.value() : response.status());
    }

    @Override
    public Object getResult() {
        if (body == null) {
            Response.Body bodied = response == null ? null : response.body();
            if (bodied == null) {
                body = new byte[0];
            } else {
                try {
                    InputStream in = bodied.asInputStream();
                    body = IOUtils.read(in);
                    Response.Builder builder = Response.builder()
                            .body(body)
                            .headers(response.headers())
                            .reason(response.reason())
                            .request(response.request())
                            .status(response.status());
                    response.close();
                    // create new
                    response = builder.build();
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
        if (key == null || response == null) {
            return null;
        }
        Map<String, Collection<String>> headers = response.headers();
        Collection<String> values = headers == null ? null : headers.get(key);
        return values == null ? null : values.iterator().next();
    }

    @Override
    public List<String> getHeaders(String key) {
        if (key == null || response == null) {
            return null;
        }
        Map<String, Collection<String>> headers = response.headers();
        Collection<String> values = headers == null ? null : headers.get(key);
        return values == null ? null : CollectionUtils.toList(values);
    }

    @Override
    public int getStatusCode() {
        return response == null ? INTERNAL_SERVER_ERROR.value() : response.status();
    }

    @Override
    public HttpStatus getHttpStatus() {
        return response == null ? INTERNAL_SERVER_ERROR : HttpStatus.resolve(response.status());
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return response == null ? new HttpHeaders() : new HttpHeaders(toMultiValueMap(getHeaders()));
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return HttpUtils.parseCookie(getHeaders(HttpHeaders.COOKIE));
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        Map<String, Collection<String>> headers = response == null ? null : response.headers();
        if (headers == null) {
            return null;
        }
        return caseInsensitive(headers, true);
    }

    /**
     * Creates degraded response using fallback configuration
     *
     * @param request       Original Feign request
     * @param degradeConfig Fallback settings including response body/headers
     * @return Preconfigured fallback response wrapper
     */
    public static FeignClusterResponse create(Request request, DegradeConfig degradeConfig) {
        byte[] data = degradeConfig.getResponseBytes();
        Map<String, Collection<String>> headers = new HashMap<>(request.headers());
        degradeConfig.append(headers);
        headers.put(HttpHeaders.CONTENT_LENGTH, Collections.singletonList(String.valueOf(data.length)));
        headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(degradeConfig.getContentType()));

        return new FeignClusterResponse(Response.builder()
                .status(degradeConfig.getResponseCode())
                .body(data)
                .headers(headers)
                .request(request)
                .requestTemplate(request.requestTemplate())
                .build());
    }

}
