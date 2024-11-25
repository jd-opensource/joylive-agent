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
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.Status;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that implements a live load balancing algorithm for distributing network traffic across multiple servers.
 */
public class LiveLoadBalancer extends LoadBalancer {

    public static final String SCHEMA_DISCOVERY = "discovery:///";
    private final Helper helper;

    private final String serviceName;

    private Map<EquivalentAddressGroup, Subchannel> subchannels = new ConcurrentHashMap<>();

    public LiveLoadBalancer(Helper helper) {
        this.helper = helper;
        this.serviceName = getServiceName(helper);
    }

    @Override
    public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
        Set<EquivalentAddressGroup> latestAddresses = new HashSet<>();
        resolvedAddresses.getAddresses().forEach(e -> e.getAddresses().forEach(a -> latestAddresses.add(new EquivalentAddressGroup(a, e.getAttributes()))));

        Map<EquivalentAddressGroup, Subchannel> newSubchannels = new ConcurrentHashMap<>();
        latestAddresses.forEach(e -> {
            Subchannel subchannel = subchannels.get(e);
            if (subchannel == null) {
                subchannel = processSubchannel(helper.createSubchannel(buildCreateSubchannelArgs(e)));
            }
            newSubchannels.put(e, subchannel);
        });

        subchannels.forEach((key, value) -> {
            if (!latestAddresses.contains(key)) {
                value.shutdown();
            }
        });

        subchannels = newSubchannels;
    }

    @Override
    public void handleNameResolutionError(Status error) {
        helper.updateBalancingState(ConnectivityState.TRANSIENT_FAILURE, new LiveSubchannelPicker(PickResult.withNoResult()));
    }

    @Override
    public void shutdown() {
        subchannels.values().forEach(Subchannel::shutdown);
    }

    protected String getServiceName(Helper helper) {
        if (helper.getClass().getName().equals("io.grpc.internal.ManagedChannelImpl$LbHelperImpl")) {
            try {
                Field field = helper.getClass().getDeclaredField("this$0");
                field.setAccessible(true);
                Object target = field.get(helper);
                field = target.getClass().getDeclaredField("target");
                field.setAccessible(true);
                target = field.get(target);
                String url = target.toString();
                if (url.startsWith(SCHEMA_DISCOVERY)) {
                    return url.substring(SCHEMA_DISCOVERY.length());
                }
            } catch (Throwable ignored) {
            }
        }
        return helper.getAuthority();
    }

    public String getServiceName() {
        return serviceName;
    }

    public Map<EquivalentAddressGroup, Subchannel> getSubchannels() {
        return subchannels;
    }

    /**
     * Builds a CreateSubchannelArgs object for creating a new subchannel.
     *
     * @param e The EquivalentAddressGroup object containing the addresses to use for the new subchannel.
     * @return A CreateSubchannelArgs object for creating a new subchannel.
     */
    private CreateSubchannelArgs buildCreateSubchannelArgs(EquivalentAddressGroup e) {
        return CreateSubchannelArgs.newBuilder()
                .setAddresses(e)
                .setAttributes(e.getAttributes().toBuilder()
                        .set(LiveRef.KEY_STATE, new LiveRef<>(ConnectivityState.IDLE))
                        .build())
                .build();
    }

    /**
     * Processes a new subchannel and adds it to the internal map of subchannels.
     *
     * @param subchannel The Subchannel object to process.
     * @return The same Subchannel object.
     */
    private Subchannel processSubchannel(Subchannel subchannel) {
        if (subchannels.containsValue(subchannel)) {
            return subchannel;
        }

        subchannel.start(new LiveSubchannelStateListener(this, subchannel, helper));
        subchannel.requestConnection();
        return subchannel;
    }
}
