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

import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import io.grpc.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint.NO_ENDPOINT_AVAILABLE;
import static io.grpc.ConnectivityState.*;

/**
 * A class that implements a live load balancing algorithm for distributing network traffic across multiple servers.
 */
public class LiveLoadBalancer extends LoadBalancer {

    public static final String SCHEMA_DISCOVERY = "discovery:///";

    private final Helper helper;

    private final Timer timer;

    private final String serviceName;

    private final AtomicBoolean submitted = new AtomicBoolean(false);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final Set<GrpcEndpoint> initializedEndpoints = new CopyOnWriteArraySet<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    private final Object mutex = new Object();

    private volatile Map<EquivalentAddressGroup, GrpcEndpoint> endpoints = new ConcurrentHashMap<>();

    public LiveLoadBalancer(Helper helper, Timer timer) {
        this.helper = helper;
        this.timer = timer;
        this.serviceName = getServiceName(helper);
    }

    public String getServiceName() {
        return serviceName;
    }

    /**
     * Handles an endpoint event by refreshing the name resolution if the event is for the same service.
     *
     * @param event the endpoint event to handle
     */
    protected void handle(RegistryEvent event) {
        if (serviceName.equals(event.getService())) {
            helper.getSynchronizationContext().execute(helper::refreshNameResolution);
        }
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
            if (initialized.compareAndSet(false, true)) {
                initializedEndpoints.addAll(added);
            }
            added.forEach(GrpcEndpoint::requestConnection);
        }
    }

    @Override
    public void handleNameResolutionError(Status error) {
        updatePicker(TRANSIENT_FAILURE, new LiveSubchannelPicker(PickResult.withDrop(error)));
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
        synchronized (mutex) {
            if (ready.compareAndSet(false, true)) {
                initializedEndpoints.clear();
            }
            submitted.compareAndSet(true, false);
            doPickReady();
        }
    }

    /**
     * Picks a new ready endpoint from the available endpoints.
     */
    private void doPickReady() {
        List<GrpcEndpoint> readies = new ArrayList<>();
        endpoints.values().forEach(endpoint -> {
            switch (endpoint.getConnectivityState()) {
                case READY:
                    readies.add(endpoint);
                    break;
                case IDLE:
                    endpoint.requestConnection();
                    break;
            }
        });
        if (readies.isEmpty()) {
            updatePicker(TRANSIENT_FAILURE, new LiveSubchannelPicker(PickResult.withDrop(
                    Status.UNAVAILABLE.withDescription(NO_ENDPOINT_AVAILABLE))));
        } else {
            updatePicker(READY, new LiveSubchannelPicker(readies));
        }
    }

    /**
     * Updates the picker with the given connectivity state and subchannel picker.
     *
     * @param state  the new connectivity state
     * @param picker the new subchannel picker
     */
    private void updatePicker(ConnectivityState state, LiveSubchannelPicker picker) {
        helper.getSynchronizationContext().execute(() -> helper.updateBalancingState(state, picker));
        LiveDiscovery.setSubchannelPicker(serviceName, picker);
    }

    /**
     * A listener for subchannel state changes that updates the connectivity state of the associated endpoint and triggers
     * the selection of a new ready endpoint if necessary.
     */
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
                if (!ready.get() && initializedEndpoints.remove(endpoint) && initializedEndpoints.isEmpty()) {
                    // all endpoints are connected.
                    tryExecute();
                } else {
                    submitTask();
                }
            }
        }

        /**
         * Attempts to execute the task of picking a new ready endpoint if all endpoints are connected.
         */
        private void tryExecute() {
            synchronized (mutex) {
                if (!ready.get()) {
                    pickReady();
                } else {
                    submitTask();
                }
            }
        }

        /**
         * Submits a task to pick a new ready endpoint if necessary.
         */
        private void submitTask() {
            if (submitted.compareAndSet(false, true)) {
                // group commit
                timer.delay("Live-PickReady", 200, LiveLoadBalancer.this::pickReady);
            }
        }
    }
}
