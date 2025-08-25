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
package com.jd.live.agent.governance.invoke.gateway;

import lombok.Getter;

import java.util.function.BiFunction;

public class GatewayRoute<R> {

    @Getter
    protected final R route;

    @Getter
    protected final Object definition;

    @Getter
    protected final long version;

    protected volatile Object reference;

    protected final Object mutex = new Object();

    public GatewayRoute(R route, Object definition, long version) {
        this.route = route;
        this.definition = definition;
        this.version = version;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreate(BiFunction<R, Long, T> function) {
        if (reference == null) {
            synchronized (mutex) {
                if (reference == null) {
                    reference = function.apply(route, version);
                }
            }
        }
        return (T) reference;
    }
}
