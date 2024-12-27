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
package com.jd.live.agent.plugin.router.gprc.loadbalance;

import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;

/**
 * Represents the result of a routing operation.
 * <p>
 * This class encapsulates either an {@link Endpoint} or a {@link Throwable} (or both) to indicate the outcome of a routing operation.
 */
public class LiveRouteResult {

    private final GrpcEndpoint endpoint;

    private final Throwable throwable;

    public LiveRouteResult(GrpcEndpoint endpoint) {
        this(endpoint, null);
    }

    public LiveRouteResult(Throwable throwable) {
        this(null, throwable);
    }

    public LiveRouteResult(GrpcEndpoint endpoint, Throwable throwable) {
        this.endpoint = endpoint;
        this.throwable = throwable;
    }

    public GrpcEndpoint getEndpoint() {
        return endpoint;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public boolean isSuccess() {
        return throwable == null;
    }
}
