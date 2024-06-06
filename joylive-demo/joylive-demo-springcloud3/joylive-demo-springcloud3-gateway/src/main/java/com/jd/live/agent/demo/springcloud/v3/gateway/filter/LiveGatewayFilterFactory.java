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

import com.jd.live.agent.demo.util.EchoResponse;
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
                                StringBuilder builder = new StringBuilder(512);
                                dataBuffers.forEach(dataBuffer -> {
                                    // probably should reuse buffers
                                    byte[] content = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(content);
                                    builder.append(new String(content, StandardCharsets.UTF_8));
                                    DataBufferUtils.release(dataBuffer);
                                });
                                EchoResponse echoResponse = new EchoResponse("spring-gateway", "header", headers::getFirst, builder.toString());
                                byte[] data = echoResponse.toString().getBytes(StandardCharsets.UTF_8);
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
