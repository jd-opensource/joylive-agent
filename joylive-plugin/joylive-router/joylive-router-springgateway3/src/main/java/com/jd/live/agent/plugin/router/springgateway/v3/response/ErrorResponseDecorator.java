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
package com.jd.live.agent.plugin.router.springgateway.v3.response;

import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.plugin.router.springcloud.v3.util.CloudUtils;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.CollectionUtils.removeAndGetFirst;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;

/**
 * A decorator for {@link ServerHttpResponse} that modifies the response body according to the specified code policies.
 */
public class ErrorResponseDecorator extends ServerHttpResponseDecorator {

    private final ServerWebExchange exchange;

    private final Set<ErrorPolicy> policies;

    private final boolean failover;

    public ErrorResponseDecorator(ServerWebExchange exchange, Set<ErrorPolicy> policies, boolean failover) {
        super(exchange.getResponse());
        this.exchange = exchange;
        this.policies = policies;
        this.failover = failover;
    }

    @NonNull
    @Override
    public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
        Map<String, Object> attributes = exchange.getAttributes();
        HttpHeaders headers = CloudUtils.writable(exchange.getResponse().getHeaders());
        ServiceError error = ServiceError.build(key -> removeAndGetFirst(headers, key));
        if (error != null) {
            attributes.put(Request.KEY_SERVER_ERROR, error);
        }
        if (policies != null && !policies.isEmpty()) {
            String contentType = (String) attributes.get(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
            if (policyMatch(contentType)) {
                if (body instanceof Flux) {
                    body = Flux.from(body).buffer().map(buffers -> readBody(buffers, attributes));
                } else if (body instanceof Mono) {
                    body = Mono.from(body).map(buffers -> readBody(buffers, attributes));
                }
            }
        }
        if (failover) {
            // Defer write operations until retry completion.
            Publisher<? extends DataBuffer> publisher = body;
            Supplier<Mono<Void>> supplier = () -> super.writeWith(publisher);
            attributes.put(Request.KEY_RESPONSE_WRITE, supplier);
            return Mono.empty();
        } else {
            return super.writeWith(body);
        }

    }

    /**
     * Reads content from multiple DataBuffers by joining them into a single buffer.
     *
     * @param buffers    the list of DataBuffers to read from
     * @param attributes the attributes map to store the response body
     * @return a new DataBuffer containing the joined content
     */
    private DataBuffer readBody(List<? extends DataBuffer> buffers, Map<String, Object> attributes) {
        DataBufferFactory factory = bufferFactory();
        return readBody(factory, factory.join(buffers), attributes);
    }

    /**
     * Reads content from a DataBuffer using the default buffer factory.
     *
     * @param buffer     the DataBuffer to read from
     * @param attributes the attributes map to store the response body
     * @return a new DataBuffer containing the content
     */
    private DataBuffer readBody(DataBuffer buffer, Map<String, Object> attributes) {
        return readBody(bufferFactory(), buffer, attributes);
    }

    /**
     * Reads content from a DataBuffer and stores it in attributes.
     * Ensures proper resource cleanup by releasing the original buffer.
     *
     * @param factory    the DataBufferFactory to create the new buffer
     * @param buffer     the DataBuffer to read from
     * @param attributes the attributes map to store the response body
     * @return a new DataBuffer containing the content
     */
    private DataBuffer readBody(DataBufferFactory factory, DataBuffer buffer, Map<String, Object> attributes) {
        byte[] content = new byte[buffer.readableByteCount()];
        try {
            buffer.read(content);
            attributes.put(Request.KEY_RESPONSE_BODY, new String(content, StandardCharsets.UTF_8));
        } finally {
            // must release the buffer
            DataBufferUtils.release(buffer);
        }
        return factory.wrap(content);
    }

    /**
     * Checks if any of the code policies match the given content type.
     *
     * @param contentType The content type to check.
     * @return true if any of the code policies match the content type, false otherwise.
     */
    private boolean policyMatch(String contentType) {
        Integer status = getRawStatusCode();
        int ok = HttpStatus.OK.value();
        for (ErrorPolicy policy : policies) {
            if (policy.match(status, contentType, ok)) {
                return true;
            }
        }
        return false;
    }
}
