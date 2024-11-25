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

import io.grpc.LoadBalancer.Subchannel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that provides methods for discovering and managing subchannels for different services.
 */
public class LiveDiscovery {

    /**
     * A map of subchannels, where the key is the service name and the value is a list of subchannels for that service.
     */
    private static final Map<String, List<Subchannel>> SUB_CHANNELS = new ConcurrentHashMap<>();

    private static final Map<String, String> SERVICES = new ConcurrentHashMap<>();

    /**
     * Returns the list of subchannels for the specified service.
     *
     * @param service The name of the service for which to retrieve the subchannels.
     * @return The list of subchannels for the specified service, or null if no subchannels are found.
     */
    public static List<Subchannel> getSubchannel(String service) {
        return SUB_CHANNELS.get(service);
    }

    /**
     * Adds or updates the list of subchannels for the specified service.
     *
     * @param service     The name of the service for which to add or update the subchannels.
     * @param subchannels The list of subchannels to add or update for the specified service.
     */
    public static void putSubchannel(String service, List<Subchannel> subchannels) {
        SUB_CHANNELS.put(service, subchannels);
    }

    public static String getService(String interfaceName) {
        return interfaceName == null || interfaceName.isEmpty() ? null : SERVICES.getOrDefault(interfaceName, interfaceName);
    }

    public static void putService(String interfaceName, String service) {
        if (interfaceName != null && !interfaceName.isEmpty() && service != null && !service.isEmpty()) {
            SERVICES.put(interfaceName, service);
        }
    }

}
