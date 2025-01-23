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
import io.grpc.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint.NO_ENDPOINT_AVAILABLE;
import static io.grpc.ConnectivityState.*;

/**
 * A class that implements a live load balancing algorithm for distributing network traffic across multiple servers.
 */
public class LiveLoadBalancer extends LoadBalancer {

    public static final String SCHEMA_DISCOVERY = "discovery:///";

    private final Helper helper;

    private final String serviceName;

    private volatile Map<EquivalentAddressGroup, GrpcEndpoint> endpoints = new ConcurrentHashMap<>();

    public LiveLoadBalancer(Helper helper) {
        this.helper = helper;
        this.serviceName = getServiceName(helper);
    }

    @Override
    public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
        Set<EquivalentAddressGroup> latestAddresses = deDup(resolvedAddresses);

        List<GrpcEndpoint> removed = new ArrayList<>();
        List<GrpcEndpoint> added = new ArrayList<>();

        Map<EquivalentAddressGroup, GrpcEndpoint> olds = endpoints;
        Map<EquivalentAddressGroup, GrpcEndpoint> news = new ConcurrentHashMap<>();
        latestAddresses.forEach(addressGroup -> {
            GrpcEndpoint endpoint = olds.get(addressGroup);
            if (endpoint == null) {
                // create new connection
                endpoint = createEndpoint(addressGroup);
                added.add(endpoint);
            }
            news.put(addressGroup, endpoint);
        });
        endpoints = news;
        olds.forEach((addressGroup, endpoint) -> {
            if (!latestAddresses.contains(addressGroup)) {
                removed.add(endpoint);
            }
        });
        // close not exists
        if (!removed.isEmpty()) {
            removed.forEach(endpoint -> {
                endpoint.setConnectivityState(SHUTDOWN);
                endpoint.shutdown();
            });
        }
        // create new connection
        if (!added.isEmpty()) {
            added.forEach(GrpcEndpoint::requestConnection);
        }
    }

    @Override
    public void handleNameResolutionError(Status error) {
//        helper.updateBalancingState(TRANSIENT_FAILURE, new LiveSubchannelPicker(PickResult.withError(error)));
        handleResolvedAddresses(null);
    }

    @Override
    public void shutdown() {
        endpoints.values().forEach(GrpcEndpoint::shutdown);
    }

    /**
     * Retrieves the service name from the specified Helper object.
     *
     * @param helper the Helper object to extract the service name from
     * @return the service name, or the authority if the service name cannot be determined
     */
    protected static String getServiceName(Helper helper) {
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

    /**
     * De-duplicates the given ResolvedAddresses by creating EquivalentAddressGroups.
     *
     * @param resolvedAddresses the ResolvedAddresses to de-duplicate
     * @return a Set of EquivalentAddressGroups containing the de-duplicated addresses
     */
    private Set<EquivalentAddressGroup> deDup(ResolvedAddresses resolvedAddresses) {
        Set<EquivalentAddressGroup> result = new HashSet<>();
        if (resolvedAddresses == null) {
            return result;
        }
        resolvedAddresses.getAddresses().forEach(addressGroup -> {
            Attributes attributes = addressGroup.getAttributes();
            addressGroup.getAddresses().forEach(address -> result.add(new EquivalentAddressGroup(address, attributes)));
        });
        return result;
    }

    /**
     * Creates a new GrpcEndpoint for the given EquivalentAddressGroup.
     *
     * @param addressGroup the EquivalentAddressGroup to create the GrpcEndpoint for
     * @return the newly created GrpcEndpoint
     */
    private GrpcEndpoint createEndpoint(EquivalentAddressGroup addressGroup) {
        LiveRef ref = new LiveRef();
        Attributes attributes = addressGroup.getAttributes().toBuilder().set(LiveRef.KEY_STATE, ref).build();
        CreateSubchannelArgs args = CreateSubchannelArgs.newBuilder().setAddresses(addressGroup).setAttributes(attributes).build();
        GrpcEndpoint endpoint = new GrpcEndpoint(helper.createSubchannel(args));
        ref.setState(IDLE);
        ref.setEndpoint(endpoint);
        endpoint.start(new LiveStateListener(endpoint));
        return endpoint;
    }

    /**
     * Picks the ready GrpcEndpoints and updates the balancing state accordingly.
     */
    private void pickReady() {
        List<GrpcEndpoint> readies = new ArrayList<>();
        endpoints.values().forEach(endpoint -> {
            if (endpoint.getConnectivityState() == ConnectivityState.READY) {
                readies.add(endpoint);
            }
        });
        if (readies.isEmpty()) {
            PickResult result = PickResult.withDrop(Status.UNAVAILABLE.withDescription(NO_ENDPOINT_AVAILABLE));
            LiveSubchannelPicker picker = new LiveSubchannelPicker(result);
            helper.updateBalancingState(ConnectivityState.CONNECTING, picker);
            LiveDiscovery.setSubchannelPicker(serviceName, picker);
        } else {
            LiveSubchannelPicker picker = new LiveSubchannelPicker(readies);
            helper.updateBalancingState(ConnectivityState.READY, picker);
            LiveDiscovery.setSubchannelPicker(serviceName, picker);
        }
    }

    private class LiveStateListener implements SubchannelStateListener {

        private final GrpcEndpoint endpoint;

        LiveStateListener(GrpcEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void onSubchannelState(ConnectivityStateInfo stateInfo) {
            ConnectivityState currentState = endpoint.getConnectivityState();
            ConnectivityState newState = stateInfo.getState();
            endpoint.setConnectivityState(newState);
            if (currentState == READY && newState != READY
                    || currentState != READY && newState == READY) {
                // call helper.updateBalancingState in this thread to avoid exception
                // syncContext.throwIfNotInThisSynchronizationContext()
                pickReady();
            }
        }
    }
}
