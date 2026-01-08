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
package com.jd.live.agent.plugin.router.springgateway.v2_2.response;

import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import com.jd.live.agent.plugin.router.springcloud.v2_2.response.SpringClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v2_2.util.CloudUtils;
import com.jd.live.agent.plugin.router.springgateway.v2_2.request.GatewayCloudClusterRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * GatewayClusterResponse
 *
 * @since 1.0.0
 */
public class GatewayClusterResponse extends AbstractHttpOutboundResponse<ServerHttpResponse> implements SpringClusterResponse {

    private final CacheObject<String> body;

    public GatewayClusterResponse(ServerWebExchange exchange) {
        this(exchange.getResponse(),
                () -> (ServiceError) exchange.getAttributes().remove(Request.KEY_SERVER_ERROR),
                () -> (String) exchange.getAttributes().remove(Request.KEY_RESPONSE_BODY));
    }

    public GatewayClusterResponse(ServerHttpResponse response) {
        this(response, null, null);
    }

    public GatewayClusterResponse(ServerHttpResponse response, Supplier<ServiceError> errorSupplier, Supplier<String> bodySupplier) {
        super(response, errorSupplier, null);
        this.body = new UnsafeLazyObject<>(bodySupplier);
    }

    public GatewayClusterResponse(ServiceError error, ErrorPredicate retryPredicate) {
        super(error, retryPredicate);
        this.body = null;
    }

    @Override
    public String getCode() {
        Integer code = response == null ? null : response.getRawStatusCode();
        return Integer.toString(code == null ? HttpStatus.INTERNAL_SERVER_ERROR.value() : code);
    }

    @Override
    public Object getResult() {
        return body == null ? null : body.get();
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
        Integer code = response == null ? null : response.getRawStatusCode();
        return code == null ? HttpStatus.INTERNAL_SERVER_ERROR.value() : code;
    }

    @Override
    public HttpStatus getHttpStatus() {
        HttpStatus status = response == null ? HttpStatus.INTERNAL_SERVER_ERROR : response.getStatusCode();
        return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return response == null ? new HttpHeaders() : response.getHeaders();
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return response == null ? null : response.getHeaders();
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return response == null ? null : HttpUtils.parseCookie(response.getCookies(), ResponseCookie::getValue);
    }

    /**
     * Creates a {@link GatewayClusterResponse} based on the provided {@link GatewayCloudClusterRequest} and {@link DegradeConfig}.
     * This method configures the response with the specified status code, headers, and content, and writes the response body.
     *
     * @param httpRequest the {@link GatewayCloudClusterRequest} containing the original request and exchange information
     * @param config      the {@link DegradeConfig} containing the response configuration (e.g., status code, headers, content)
     * @return a new {@link GatewayClusterResponse} representing the degraded response
     */
    public static GatewayClusterResponse create(GatewayCloudClusterRequest httpRequest, DegradeConfig config) {
        ServerHttpResponse response = httpRequest.getExchange().getResponse();
        ServerHttpRequest request = httpRequest.getExchange().getRequest();

        DataBuffer buffer = response.bufferFactory().wrap(config.getResponseBytes());
        HttpHeaders headers = CloudUtils.writable(response.getHeaders());
        headers.putAll(request.getHeaders());
        config.foreach(headers::add);
        response.setRawStatusCode(config.getResponseCode());
        response.setStatusCode(HttpStatus.valueOf(config.getResponseCode()));
        headers.set(HttpHeaders.CONTENT_TYPE, config.contentTypeOrDefault());

        response.writeWith(Flux.just(buffer)).subscribe();
        return new GatewayClusterResponse(response);
    }
}
