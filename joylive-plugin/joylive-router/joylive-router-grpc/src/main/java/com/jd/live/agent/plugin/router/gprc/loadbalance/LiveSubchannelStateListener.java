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

import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.LoadBalancer.SubchannelStateListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that implements the SubchannelStateListener interface to listen for changes in the state of a subchannel.
 */
public class LiveSubchannelStateListener implements SubchannelStateListener {

    private final Subchannel subchannel;
    private final LoadBalancer.Helper helper;
    private final LiveLoadBalancer loadBalancer;

    public LiveSubchannelStateListener(LiveLoadBalancer loadBalancer,
                                       Subchannel subchannel,
                                       LoadBalancer.Helper helper) {
        this.loadBalancer = loadBalancer;
        this.subchannel = subchannel;
        this.helper = helper;
    }

    @Override
    public void onSubchannelState(ConnectivityStateInfo stateInfo) {
        LiveRef<ConnectivityState> ref = subchannel.getAttributes().get(LiveRef.KEY_STATE);
        ConnectivityState currentState = ref.getValue();
        ConnectivityState newState = stateInfo.getState();

        if (newState == ConnectivityState.SHUTDOWN) {
            return;
        }
        if (newState == ConnectivityState.READY) {
            subchannel.requestConnection();
        }
        if (currentState == ConnectivityState.TRANSIENT_FAILURE) {
            if (newState == ConnectivityState.CONNECTING || newState == ConnectivityState.IDLE) {
                return;
            }
        }

        ref.setValue(newState);
        updateLoadBalancerState();
    }

    private void updateLoadBalancerState() {
        List<Subchannel> readies = new ArrayList<>();

        loadBalancer.getSubchannels().values().forEach(e -> {
            if (e.getAttributes().get(LiveRef.KEY_STATE).getValue() == ConnectivityState.READY) {
                readies.add(e);
            }
        });
        LiveDiscovery.putSubchannel(loadBalancer.getServiceName(), readies);
        if (readies.isEmpty()) {
            helper.updateBalancingState(ConnectivityState.CONNECTING, new LiveSubchannelPicker(PickResult.withNoResult()));
        } else {
            helper.updateBalancingState(ConnectivityState.READY, new LiveSubchannelPicker(readies));
        }
    }

}
