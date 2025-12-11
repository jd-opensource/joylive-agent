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
package com.jd.live.agent.implement.bean.mcp.converter;

import com.jd.live.agent.core.mcp.spec.v1.CallToolResult;
import com.jd.live.agent.core.mcp.spec.v1.JsonRpcResponse;
import com.jd.live.agent.core.util.converter.Converter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Converter that transforms various types of objects into Mono instances.
 * Handles reactive types, completion stages, and JsonRpcResponse objects.
 */
public class MonoConverter implements Converter<Object, Object> {

    /**
     * Singleton instance of the converter
     */
    public static final MonoConverter INSTANCE = new MonoConverter();

    @Override
    public Mono<Object> convert(Object source) {
        Function<Object, Object> map = null;
        Function<Throwable, Mono<Object>> errorResume = null;
        Object target = source;
        if (target instanceof JsonRpcResponse) {
            JsonRpcResponse response = (JsonRpcResponse) source;
            if (response.success() && response.getResult() instanceof CallToolResult) {
                CallToolResult result = (CallToolResult) response.getResult();
                Boolean error = result.getError();
                if (error == null || !error) {
                    target = result.getStructuredContent();
                    map = o -> result.structuredContent(o);
                    errorResume = e -> Mono.just(response.result(new CallToolResult(e)));
                }
            }
        }
        if (target instanceof Mono) {
            Mono<Object> mono = (Mono<Object>) target;
            return map == null ? mono : mono.map(map).onErrorResume(errorResume);
        } else if (target instanceof Publisher) {
            Mono<Object> mono = Mono.from((Publisher<Object>) target);
            return map == null ? mono : mono.map(map).onErrorResume(errorResume);
        } else if (target instanceof CompletionStage) {
            Mono<Object> mono = Mono.fromCompletionStage((CompletionStage<Object>) target);
            return map == null ? mono : mono.map(map).onErrorResume(errorResume);
        }
        return Mono.just(source);
    }
}
