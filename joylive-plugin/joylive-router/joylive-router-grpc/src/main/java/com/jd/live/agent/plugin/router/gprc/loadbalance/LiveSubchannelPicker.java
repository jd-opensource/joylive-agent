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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.plugin.router.gprc.exception.GrpcStatus;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.SubchannelPicker;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class that extends the SubchannelPicker class to provide a live subchannel picking strategy.
 */
public class LiveSubchannelPicker extends SubchannelPicker {

    private static final Logger logger = LoggerFactory.getLogger(LiveSubchannelPicker.class);

    private final PickResult pickResult;

    private final List<LiveSubchannel> subchannels;

    private final AtomicLong counter = new AtomicLong();

    public LiveSubchannelPicker(PickResult pickResult) {
        this(pickResult, null);
    }

    public LiveSubchannelPicker(List<LiveSubchannel> subchannels) {
        this(null, subchannels);
    }

    public LiveSubchannelPicker(PickResult pickResult, List<LiveSubchannel> subchannels) {
        this.pickResult = pickResult;
        this.subchannels = subchannels;
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
        LiveRequest request = args.getCallOptions().getOption(LiveRequest.KEY_LIVE_REQUEST);
        if (pickResult != null && request != null) {
            request.setEndpoint(pickResult.getSubchannel() == null
                    ? null
                    : new GrpcEndpoint(new LiveSubchannel(pickResult.getSubchannel())));
            return pickResult;
        } else if (pickResult != null) {
            return pickResult;
        } else if (request != null) {
            try {
                GrpcEndpoint endpoint = request.route(subchannels);
                return endpoint == null
                        ? PickResult.withNoResult()
                        : PickResult.withSubchannel(endpoint.getSubchannel().getSubchannel());
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                return PickResult.withError(GrpcStatus.createException(e));
            }
        } else {
            long v = counter.getAndIncrement();
            if (v < 0) {
                counter.set(0);
                v = counter.getAndIncrement();
            }
            int index = (int) (v % subchannels.size());
            return PickResult.withSubchannel(subchannels.get(index).getSubchannel());
        }

    }
}
