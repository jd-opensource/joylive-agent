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

    private volatile Map<EquivalentAddressGroup, LiveSubchannel> subchannels = new ConcurrentHashMap<>();

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

        List<LiveSubchannel> removed = new ArrayList<>();
        List<LiveSubchannel> added = new ArrayList<>();

        Map<EquivalentAddressGroup, LiveSubchannel> oldSubchannels = subchannels;
        Map<EquivalentAddressGroup, LiveSubchannel> newSubchannels = new ConcurrentHashMap<>();
        latestAddresses.forEach(addressGroup -> {
            LiveSubchannel subchannel = oldSubchannels.get(addressGroup);
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
                subchannel.setConnectivityState(SHUTDOWN);
                subchannel.shutdown();
            });
        }
        // create new connection
        if (!added.isEmpty()) {
            added.forEach(LiveSubchannel::requestConnection);
        }
    }

    @Override
    public void handleNameResolutionError(Status error) {
        helper.updateBalancingState(TRANSIENT_FAILURE, new LiveSubchannelPicker(PickResult.withNoResult()));
    }

    @Override
    public void shutdown() {
        subchannels.values().forEach(LiveSubchannel::shutdown);
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
    private LiveSubchannel createSubchannel(EquivalentAddressGroup addressGroup) {
        Attributes attributes = addressGroup.getAttributes().toBuilder().set(LiveRef.KEY_STATE, new LiveRef<>(IDLE)).build();
        CreateSubchannelArgs args = CreateSubchannelArgs.newBuilder().setAddresses(addressGroup).setAttributes(attributes).build();
        LiveSubchannel subchannel = new LiveSubchannel(helper.createSubchannel(args));
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
        List<LiveSubchannel> readies = new ArrayList<>();
        subchannels.values().forEach(subchannel -> {
            if (subchannel.getConnectivityState() == ConnectivityState.READY) {
                readies.add(subchannel);
            }
        });
        if (readies.isEmpty()) {
            LiveSubchannelPicker picker = new LiveSubchannelPicker(PickResult.withNoResult());
            helper.updateBalancingState(ConnectivityState.CONNECTING, picker);
            LiveDiscovery.setSubchannelPicker(serviceName, picker);
        } else {
            LiveSubchannelPicker picker = new LiveSubchannelPicker(readies);
            helper.updateBalancingState(ConnectivityState.READY, picker);
            LiveDiscovery.setSubchannelPicker(serviceName, picker);
        }
    }

    private class LiveSubchannelStateListener implements SubchannelStateListener {

        private final LiveSubchannel subchannel;

        LiveSubchannelStateListener(LiveSubchannel subchannel) {
            this.subchannel = subchannel;
        }

        @Override
        public void onSubchannelState(ConnectivityStateInfo stateInfo) {
            ConnectivityState currentState = subchannel.getConnectivityState();
            ConnectivityState newState = stateInfo.getState();
            subchannel.setConnectivityState(newState);
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
