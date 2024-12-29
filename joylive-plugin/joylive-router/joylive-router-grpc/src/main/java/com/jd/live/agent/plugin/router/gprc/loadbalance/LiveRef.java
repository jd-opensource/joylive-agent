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

import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import io.grpc.Attributes.Key;
import io.grpc.ConnectivityState;

/**
 * Represents a live reference to a gRPC endpoint with its connectivity state.
 *
 * This class contains a gRPC endpoint and its current connectivity state, providing methods to get and set these values.
 */
public final class LiveRef {

    public static final Key<LiveRef> KEY_STATE = Key.create("x-state");

    private GrpcEndpoint endpoint;

    private ConnectivityState state;

    public GrpcEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(GrpcEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public ConnectivityState getState() {
        return state;
    }

    public void setState(ConnectivityState state) {
        this.state = state;
    }

}
