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
package com.jd.live.agent.plugin.router.springgateway.v2_1.filter;

import lombok.Getter;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.server.ServerWebExchange;

public class LiveRoutePredicate implements AsyncPredicate<ServerWebExchange> {

    private final AsyncPredicate<ServerWebExchange> delegate;

    @Getter
    private final RouteDefinition definition;

    @Getter
    private final long version;

    public LiveRoutePredicate(AsyncPredicate<ServerWebExchange> delegate, long version) {
        this(delegate, null, version);
    }

    public LiveRoutePredicate(AsyncPredicate<ServerWebExchange> delegate, RouteDefinition definition, long version) {
        this.delegate = delegate;
        this.definition = definition;
        this.version = version;
    }

    @Override
    public Publisher<Boolean> apply(ServerWebExchange serverWebExchange) {
        return delegate.apply(serverWebExchange);
    }
}
