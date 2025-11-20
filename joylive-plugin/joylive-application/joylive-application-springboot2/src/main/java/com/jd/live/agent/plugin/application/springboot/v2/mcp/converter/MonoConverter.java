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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.converter;

import com.jd.live.agent.core.util.converter.Converter;
import com.jd.live.agent.governance.mcp.spec.JsonRpcResponse;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

public class MonoConverter implements Converter<Object, Object> {

    public static final MonoConverter INSTANCE = new MonoConverter();

    @Override
    public Mono<Object> convert(Object source) {
        if (source == null) {
            return Mono.empty();
        } else if (source instanceof Mono) {
            return (Mono<Object>) source;
        } else if (source instanceof Publisher) {
            return Mono.from((Publisher<Object>) source);
        } else if (source instanceof CompletionStage) {
            return Mono.fromCompletionStage((CompletionStage<Object>) source);
        } else if (source instanceof JsonRpcResponse) {
            JsonRpcResponse response = (JsonRpcResponse) source;
            if (response.isSuccess()) {
                Object result = response.getResult();
                if (result instanceof Mono) {
                    return (Mono<Object>) result;
                } else if (result instanceof Publisher) {
                    return Mono.from((Publisher<Object>) result);
                } else if (result instanceof CompletionStage) {
                    return Mono.fromCompletionStage((CompletionStage<Object>) result);
                }
            }
        }
        return Mono.just(source);
    }
}
