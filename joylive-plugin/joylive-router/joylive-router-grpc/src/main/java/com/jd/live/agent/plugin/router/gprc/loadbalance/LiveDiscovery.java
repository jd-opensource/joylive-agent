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
import io.grpc.LoadBalancer.SubchannelPicker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that provides methods for discovering and managing picker for different services.
 */
public class LiveDiscovery {

    private static final Map<String, Picker> PICKERS = new ConcurrentHashMap<>();

    private static final Map<String, String> SERVICES = new ConcurrentHashMap<>();

    private static final int INITIALIZE_TIMEOUT_MILLIS = 10000;

    /**
     * Retrieves the SubchannelPicker for the specified service.
     *
     * @param service The name of the service.
     * @return The SubchannelPicker associated with the specified service.
     */
    public static SubchannelPicker getSubchannelPicker(String service) {
        return getPicker(service).getPicker();
    }

    /**
     * Sets the SubchannelPicker for the specified service.
     *
     * @param service The name of the service.
     * @param picker The SubchannelPicker to be set for the specified service.
     */
    public static void setSubchannelPicker(String service, SubchannelPicker picker) {
        getPicker(service).setPicker(picker);
    }

    /**
     * Retrieves the service name associated with the given interface name.
     *
     * @param interfaceName The name of the interface.
     * @return The service name associated with the interface name, or the interface name itself if no service is found.
     */
    public static String getService(String interfaceName) {
        return interfaceName == null || interfaceName.isEmpty() ? null : SERVICES.getOrDefault(interfaceName, interfaceName);
    }

    /**
     * Associates a service name with the given interface name.
     *
     * @param interfaceName The name of the interface.
     * @param service       The name of the service to be associated with the interface name.
     */
    public static void putService(String interfaceName, String service) {
        if (interfaceName != null && !interfaceName.isEmpty() && service != null && !service.isEmpty()) {
            SERVICES.put(interfaceName, service);
        }
    }

    private static Picker getPicker(String service) {
        return PICKERS.computeIfAbsent(service, Picker::new);
    }

    /**
     * A class that manages a gRPC SubchannelPicker with thread-safe updates and retrieval.
     * <p>
     * This class is responsible for maintaining a service identifier, an update time, and a SubchannelPicker.
     * It provides methods to set and get the SubchannelPicker, ensuring thread safety and proper initialization.
     */
    private static class Picker {

        private static final Logger logger = LoggerFactory.getLogger(Picker.class);

        private final String service;

        private volatile long updateTime;

        private volatile SubchannelPicker picker;

        private final Object mutex = new Object();

        Picker(String service) {
            this.service = service;
        }

        public String getService() {
            return service;
        }

        public void setPicker(SubchannelPicker picker) {
            this.picker = picker;
            long lastUpdateTime = updateTime;
            this.updateTime = System.currentTimeMillis();
            if (lastUpdateTime == 0) {
                synchronized (mutex) {
                    mutex.notifyAll();
                }
            }
        }

        public SubchannelPicker getPicker() {
            if (updateTime == 0) {
                // wait for initialization
                synchronized (mutex) {
                    if (updateTime == 0) {
                        try {
                            mutex.wait(INITIALIZE_TIMEOUT_MILLIS);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
            if (picker == null) {
                logger.warn("[LiveDiscovery]Picker is null, service {}", service);
            }
            return picker;
        }
    }

}
