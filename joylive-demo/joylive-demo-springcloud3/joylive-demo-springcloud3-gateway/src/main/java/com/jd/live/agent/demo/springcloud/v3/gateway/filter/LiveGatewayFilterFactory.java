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
package com.jd.live.agent.demo.springcloud.v3.gateway.filter;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class LiveGatewayFilterFactory extends AbstractGatewayFilterFactory<LiveGatewayFilterFactory.Config> {

    public LiveGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public String name() {
        return "Live";
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter(new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();
                ServerHttpResponseDecorator decorator = new ServerHttpResponseDecorator(response) {
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                            return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                                HttpHeaders headers = request.getHeaders();
                                StringBuilder builder = new StringBuilder("spring-gateway:\n").
                                        append("  header:{").
                                        append("x-live-space-id=").append(headers.getFirst("x-live-space-id")).
                                        append(", x-live-rule-id=").append(headers.getFirst("x-live-rule-id")).
                                        append(", x-live-uid=").append(headers.getFirst("x-live-uid")).
                                        append(", x-lane-space-id=").append(headers.getFirst("x-lane-space-id")).
                                        append(", x-lane-code=").append(headers.getFirst("x-lane-code")).
                                        append("}\n").
                                        append("  location:{").
                                        append("liveSpaceId=").append(System.getProperty("x-live-space-id")).
                                        append(", unit=").append(System.getProperty("x-live-unit")).
                                        append(", cell=").append(System.getProperty("x-live-cell")).
                                        append(", laneSpaceId=").append(System.getProperty("x-lane-space-id")).
                                        append(", lane=").append(System.getProperty("x-lane-code")).
                                        append("}\n\n");
                                dataBuffers.forEach(dataBuffer -> {
                                    // probably should reuse buffers
                                    byte[] content = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(content);
                                    builder.append(new String(content, StandardCharsets.UTF_8));
                                    DataBufferUtils.release(dataBuffer);
                                });
                                byte[] data = builder.toString().getBytes(StandardCharsets.UTF_8);
                                response.getHeaders().setContentLength(data.length);
                                return response.bufferFactory().wrap(data);
                            }));
                        }
                        return super.writeWith(body);
                    }
                };

                return chain.filter(exchange.mutate().response(decorator).build());
            }
        }, -2);
    }

    public static class Config {

    }
}
