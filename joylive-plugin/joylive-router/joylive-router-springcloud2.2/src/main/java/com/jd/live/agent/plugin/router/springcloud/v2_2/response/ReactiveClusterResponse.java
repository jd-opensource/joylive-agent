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

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import com.jd.live.agent.plugin.router.springcloud.v2_2.request.DegradeHttpRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_2.request.ReactiveCloudClusterRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.util.List;
import java.util.Map;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * ReactiveRouteResponse
 *
 * @since 1.0.0
 */
public class ReactiveClusterResponse extends AbstractHttpOutboundResponse<ClientResponse> implements SpringClusterResponse {

    private String body;

    public ReactiveClusterResponse(ClientResponse response) {
        super(response);
    }

    public ReactiveClusterResponse(ServiceError error, ErrorPredicate predicate) {
        super(error, predicate);
    }

    @Override
    public String getCode() {
        int code = response != null ? response.rawStatusCode() : INTERNAL_SERVER_ERROR.value();
        return Integer.toString(code);
    }

    @Override
    public Object getResult() {
        if (body == null) {
            if (response == null) {
                body = "";
            } else {
                body = response.bodyToMono(String.class).block();
                response = ClientResponse.from(response).body(body).build();
            }
        }
        return body;
    }

    @Override
    public List<String> getHeaders(String key) {
        return response == null ? null : response.headers().header(key);
    }

    @Override
    public int getStatusCode() {
        return response == null ? INTERNAL_SERVER_ERROR.value() : response.rawStatusCode();
    }

    @Override
    public HttpStatus getHttpStatus() {
        return response == null ? INTERNAL_SERVER_ERROR : response.statusCode();
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return response == null ? new HttpHeaders() : response.headers().asHttpHeaders();
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        MultiValueMap<String, ResponseCookie> cookies = response == null ? null : response.cookies();
        return cookies == null ? null : HttpUtils.parseCookie(cookies, ResponseCookie::getValue);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return response == null ? null : response.headers().asHttpHeaders();
    }

    /**
     * Builds a response object based on the client request, degrade configuration.
     *
     * @param request       Client request information.
     * @param degradeConfig Degrade configuration, containing response code, body length, body content, and headers.
     * @return The constructed response object.
     */
    public static ReactiveClusterResponse create(ReactiveCloudClusterRequest request, DegradeConfig degradeConfig) {
        ExchangeStrategies strategies;
        try {
            ExchangeFunction next = request.getNext();
            FieldAccessor fieldAccessor = getAccessor(next.getClass(), "strategies");
            strategies = fieldAccessor == null ? ExchangeStrategies.withDefaults() : (ExchangeStrategies) fieldAccessor.get(next);
        } catch (Throwable ignored) {
            strategies = ExchangeStrategies.withDefaults();
        }
        return create(request.getRequest(), degradeConfig, strategies);
    }

    /**
     * Builds a response object based on the client request, degrade configuration, and exchange strategies.
     *
     * @param request       Client request information.
     * @param degradeConfig Degrade configuration, containing response code, body length, body content, and headers.
     * @param strategies    Exchange strategies for handling requests and responses.
     * @return The constructed response object.
     */
    public static ReactiveClusterResponse create(ClientRequest request,
                                                 DegradeConfig degradeConfig,
                                                 ExchangeStrategies strategies) {
        int length = degradeConfig.bodyLength();
        return new ReactiveClusterResponse(ClientResponse.create(degradeConfig.getResponseCode(), strategies)
                .body(length == 0 ? "" : degradeConfig.getResponseBody())
                .request(new DegradeHttpRequest(request))
                .headers(headers -> {
                    headers.addAll(request.headers());
                    degradeConfig.foreach(headers::add);
                    headers.set(HttpHeaders.CONTENT_TYPE, degradeConfig.contentTypeOrDefault());
                    headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
                }).build());
    }

}
