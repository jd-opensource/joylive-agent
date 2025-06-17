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
package com.jd.live.agent.plugin.router.springgateway.v4.response;

import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.request.Request;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;

/**
 * A decorator for {@link ServerHttpResponse} that modifies the response body according to the specified code policies.
 */
public class ErrorResponseDecorator extends ServerHttpResponseDecorator {

    private final ServerWebExchange exchange;

    private final Set<ErrorPolicy> policies;

    public ErrorResponseDecorator(ServerWebExchange exchange, Set<ErrorPolicy> policies) {
        super(exchange.getResponse());
        this.exchange = exchange;
        this.policies = policies;
    }

    @NonNull
    @Override
    public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
        final HttpHeaders headers = HttpHeaders.writableHttpHeaders(exchange.getResponse().getHeaders());
        ServiceError error = ServiceError.build(key -> {
            List<String> values = headers.remove(key);
            return values == null || values.isEmpty() ? null : values.get(0);
        });
        if (error != null) {
            exchange.getAttributes().put(Request.KEY_SERVER_ERROR, error);
        }
        if (policies != null && !policies.isEmpty()) {
            String contentType = exchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
            if (body instanceof Flux && policyMatch(contentType)) {
                Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                    DataBufferFactory bufferFactory = bufferFactory();
                    DataBuffer join = bufferFactory.join(dataBuffers);
                    byte[] content = new byte[join.readableByteCount()];
                    join.read(content);
                    // must release the buffer
                    DataBufferUtils.release(join);
                    exchange.getAttributes().put(Request.KEY_RESPONSE_BODY, new String(content, StandardCharsets.UTF_8));
                    return bufferFactory.wrap(content);
                }));
            }
        }
        return super.writeWith(body);
    }

    /**
     * Checks if any of the code policies match the given content type.
     *
     * @param contentType The content type to check.
     * @return true if any of the code policies match the content type, false otherwise.
     */
    private boolean policyMatch(String contentType) {
        contentType = contentType == null ? null : contentType.toLowerCase();
        HttpStatusCode statusCode = getStatusCode();
        Integer status = statusCode == null ? null : statusCode.value();
        int ok = HttpStatus.OK.value();
        for (ErrorPolicy policy : policies) {
            if (policy.match(status, contentType, ok)) {
                return true;
            }
        }
        return false;
    }
}
