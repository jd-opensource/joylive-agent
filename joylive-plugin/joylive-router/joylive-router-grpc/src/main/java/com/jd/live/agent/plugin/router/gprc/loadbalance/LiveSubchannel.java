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

import io.grpc.Attributes;
import io.grpc.ConnectivityState;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.LoadBalancer.SubchannelStateListener;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import static io.grpc.ConnectivityState.IDLE;

/**
 * A class that wrap subchannel.
 */
public class LiveSubchannel {

    private final Subchannel subchannel;

    private InetSocketAddress address;

    public LiveSubchannel(Subchannel subchannel) {
        this.subchannel = subchannel;
    }

    public Subchannel getSubchannel() {
        return subchannel;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public Attributes getAttributes() {
        return subchannel.getAttributes();
    }

    public void requestConnection() {
        subchannel.requestConnection();
    }

    public void shutdown() {
        subchannel.shutdown();
    }

    public void start(SubchannelStateListener listener) {
        subchannel.start(listener);
        address = getInetSocketAddress(subchannel);
    }

    /**
     * Gets the current ConnectivityState.
     *
     * @return the current ConnectivityState, or IDLE if no state is set
     */
    public ConnectivityState getConnectivityState() {
        LiveRef<ConnectivityState> ref = subchannel.getAttributes().get(LiveRef.KEY_STATE);
        return ref == null ? IDLE : ref.getValue();
    }

    /**
     * Sets the ConnectivityState to the specified newState.
     *
     * @param newState the new ConnectivityState to set
     */
    public void setConnectivityState(ConnectivityState newState) {
        LiveRef<ConnectivityState> ref = subchannel.getAttributes().get(LiveRef.KEY_STATE);
        if (ref != null) {
            ref.setValue(newState);
        }
    }

    private static InetSocketAddress getInetSocketAddress(Subchannel subchannel) {

        List<SocketAddress> addresses = subchannel.getAllAddresses().get(0).getAddresses();
        for (SocketAddress addr : addresses) {
            if (addr instanceof InetSocketAddress) {
                return (InetSocketAddress) addr;
            }
        }
        return null;
    }
}
