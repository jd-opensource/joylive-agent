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
import io.grpc.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static io.grpc.ConnectivityState.*;

/**
 * A class that implements a live load balancing algorithm for distributing network traffic across multiple servers.
 */
public class LiveLoadBalancer extends LoadBalancer {

    public static final String SCHEMA_DISCOVERY = "discovery:///";

    private final Helper helper;

    private final String serviceName;

    private final Timer timer;

    private volatile Map<EquivalentAddressGroup, Subchannel> subchannels = new ConcurrentHashMap<>();

    private final AtomicLong versions = new AtomicLong(0);

    private final AtomicBoolean taskAdded = new AtomicBoolean(false);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public LiveLoadBalancer(Helper helper, Timer timer) {
        this.helper = helper;
        this.timer = timer;
        this.serviceName = getServiceName(helper);
    }

    @Override
    public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
        Set<EquivalentAddressGroup> latestAddresses = deDup(resolvedAddresses);

        List<Subchannel> removed = new ArrayList<>();
        List<Subchannel> added = new ArrayList<>();

        Map<EquivalentAddressGroup, Subchannel> oldSubchannels = subchannels;
        Map<EquivalentAddressGroup, Subchannel> newSubchannels = new ConcurrentHashMap<>();
        latestAddresses.forEach(addressGroup -> {
            Subchannel subchannel = oldSubchannels.get(addressGroup);
            if (subchannel == null) {
                // create new connection
                subchannel = createSubchannel(addressGroup);
                added.add(subchannel);
            }
            newSubchannels.put(addressGroup, subchannel);
        });
        subchannels = newSubchannels;
        oldSubchannels.forEach((addressGroup, subchannel) -> {
            if (!latestAddresses.contains(addressGroup)) {
                removed.remove(subchannel);
            }
        });
        // close not exists
        if (!removed.isEmpty()) {
            addTask();
            removed.forEach(subchannel -> {
                setConnectivityState(subchannel, SHUTDOWN);
                subchannel.shutdown();
            });
        }
        // create new connection
        if (!added.isEmpty()) {
            added.forEach(Subchannel::requestConnection);
        }
    }

    @Override
    public void handleNameResolutionError(Status error) {
        helper.updateBalancingState(TRANSIENT_FAILURE, new LiveSubchannelPicker(PickResult.withNoResult()));
    }

    @Override
    public void shutdown() {
        subchannels.values().forEach(Subchannel::shutdown);
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
        resolvedAddresses.getAddresses().forEach(addressGroup -> {
            Attributes attributes = addressGroup.getAttributes();
            addressGroup.getAddresses().forEach(address -> result.add(new EquivalentAddressGroup(address, attributes)));
        });
        return result;
    }

    /**
     * Creates a new Subchannel for the given EquivalentAddressGroup.
     *
     * @param addressGroup the EquivalentAddressGroup to create the Subchannel for
     * @return the newly created Subchannel
     */
    private Subchannel createSubchannel(EquivalentAddressGroup addressGroup) {
        Attributes attributes = addressGroup.getAttributes().toBuilder().set(LiveRef.KEY_STATE, new LiveRef<>(IDLE)).build();
        CreateSubchannelArgs args = CreateSubchannelArgs.newBuilder().setAddresses(addressGroup).setAttributes(attributes).build();
        Subchannel subchannel = helper.createSubchannel(args);
        subchannel.start(new LiveSubchannelStateListener(subchannel));
        return subchannel;
    }

    /**
     * Adds a task to update the ready instances and schedules it for execution.
     */
    private void addTask() {
        versions.incrementAndGet();
        submitTask();
    }

    /**
     * Submits a task to update the ready instances and schedules it for execution.
     *
     * @see #addTask()
     */
    private void submitTask() {
        if (taskAdded.compareAndSet(false, true)) {
            String name = "update-ready-instance-" + serviceName;
            int interval = 1000 + ThreadLocalRandom.current().nextInt(2000);
            timer.add(name, interval, () -> {
                long version = versions.get();
                pickReady();
                taskAdded.set(false);
                if (versions.get() != version) {
                    submitTask();
                }
            });
        }
    }

    /**
     * Picks the ready Subchannels and updates the balancing state accordingly.
     */
    private void pickReady() {
        List<Subchannel> readies = new ArrayList<>();
        subchannels.values().forEach(subchannel -> {
            if (getConnectivityState(subchannel) == ConnectivityState.READY) {
                readies.add(subchannel);
            }
        });
        LiveDiscovery.putSubchannel(serviceName, readies);
        if (readies.isEmpty()) {
            helper.updateBalancingState(ConnectivityState.CONNECTING, new LiveSubchannelPicker(PickResult.withNoResult()));
        } else {
            helper.updateBalancingState(ConnectivityState.READY, new LiveSubchannelPicker(readies));
        }
    }

    /**
     * Gets the current ConnectivityState of the given Subchannel.
     *
     * @param subchannel the Subchannel to get the ConnectivityState for
     * @return the current ConnectivityState, or IDLE if no state is set
     */
    private ConnectivityState getConnectivityState(Subchannel subchannel) {
        LiveRef<ConnectivityState> ref = subchannel.getAttributes().get(LiveRef.KEY_STATE);
        return ref == null ? IDLE : ref.getValue();
    }

    /**
     * Sets the ConnectivityState of the given Subchannel to the specified newState.
     *
     * @param subchannel the Subchannel to set the ConnectivityState for
     * @param newState the new ConnectivityState to set
     */
    private void setConnectivityState(Subchannel subchannel, ConnectivityState newState) {
        LiveRef<ConnectivityState> ref = subchannel.getAttributes().get(LiveRef.KEY_STATE);
        if (ref != null) {
            ref.setValue(newState);
        }
    }

    private class LiveSubchannelStateListener implements SubchannelStateListener {

        private final Subchannel subchannel;

        LiveSubchannelStateListener(Subchannel subchannel) {
            this.subchannel = subchannel;
        }

        @Override
        public void onSubchannelState(ConnectivityStateInfo stateInfo) {
            ConnectivityState currentState = getConnectivityState(subchannel);
            ConnectivityState newState = stateInfo.getState();
            setConnectivityState(subchannel, newState);
            if (currentState == READY && newState != READY
                    || currentState != READY && newState == READY) {
                if (initialized.compareAndSet(false, true)) {
                    pickReady();
                } else {
                    // Asynchronous processing, preventing massive up and down
                    addTask();
                }
            }
        }
    }
}
