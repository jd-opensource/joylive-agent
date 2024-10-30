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
package com.jd.live.agent.governance.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for Predicates.
 */
public class Predicates {

    private static final String DUBBO_APACHE_METADATA_SERVICE = "org.apache.dubbo.metadata.MetadataService";

    private static final String DUBBO_APACHE_REGISTRY_SERVICE = "org.apache.dubbo.registry.RegistryService";

    private static final String DUBBO_APACHE_MONITOR_SERVICE = "org.apache.dubbo.monitor.MonitorService";

    private static final String DUBBO_ALIBABA_REGISTRY_SERVICE = "com.alibaba.dubbo.registry.RegistryService";

    private static final String DUBBO_ALIBABA_MONITOR_SERVICE = "com.alibaba.dubbo.monitor.MonitorService";

    private static final Set<String> DUBBO_SYSTEM_SERVICES = new HashSet<>(Arrays.asList(
            DUBBO_APACHE_METADATA_SERVICE, DUBBO_APACHE_REGISTRY_SERVICE, DUBBO_APACHE_MONITOR_SERVICE,
            DUBBO_ALIBABA_REGISTRY_SERVICE, DUBBO_ALIBABA_MONITOR_SERVICE));

    /**
     * Checks if the given service name is a system service for Dubbo.
     *
     * @param serviceName the name of the service to check.
     * @return true if the service name is a system service for Dubbo, false otherwise.
     */
    public static boolean isDubboSystemService(String serviceName) {
        return serviceName != null && DUBBO_SYSTEM_SERVICES.contains(serviceName);
    }

}
