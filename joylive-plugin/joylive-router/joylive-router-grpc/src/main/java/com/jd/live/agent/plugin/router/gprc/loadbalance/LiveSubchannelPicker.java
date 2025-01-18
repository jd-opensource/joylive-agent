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

import com.jd.live.agent.plugin.router.gprc.exception.GrpcStatus;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.SubchannelPicker;
import io.grpc.Status;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint.NO_ENDPOINT_AVAILABLE;
import static com.jd.live.agent.plugin.router.gprc.loadbalance.LiveRequest.KEY_LIVE_REQUEST;

/**
 * A class that extends the SubchannelPicker class to provide a live subchannel picking strategy.
 */
public class LiveSubchannelPicker extends SubchannelPicker {

    private final PickResult pickResult;

    private final List<GrpcEndpoint> endpoints;

    private final AtomicLong counter = new AtomicLong();

    public LiveSubchannelPicker(PickResult pickResult) {
        this(pickResult, null);
    }

    public LiveSubchannelPicker(List<GrpcEndpoint> endpoints) {
        this(null, endpoints);
    }

    public LiveSubchannelPicker(PickResult pickResult, List<GrpcEndpoint> endpoints) {
        this.pickResult = pickResult;
        this.endpoints = endpoints;
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
        LiveRequest<?, ?> request = args.getCallOptions().getOption(KEY_LIVE_REQUEST);
        if (pickResult != null && request != null) {
            request.route(pickResult);
            return pickResult;
        } else if (pickResult != null) {
            return pickResult;
        } else if (request != null) {
            request.route(endpoints);
            LiveRouteResult result = request.getRouteResult();
            if (result.isSuccess()) {
                GrpcEndpoint endpoint = result.getEndpoint();
                return endpoint == null
                        ? PickResult.withDrop(Status.UNAVAILABLE.withDescription(NO_ENDPOINT_AVAILABLE))
                        : PickResult.withSubchannel(endpoint.getSubchannel());
            } else {
                return PickResult.withError(GrpcStatus.createException(result.getThrowable()));
            }
        } else {
            long v = counter.getAndIncrement();
            if (v < 0) {
                counter.set(0);
                v = counter.getAndIncrement();
            }
            int index = (int) (v % endpoints.size());
            return PickResult.withSubchannel(endpoints.get(index).getSubchannel());
        }

    }
}
