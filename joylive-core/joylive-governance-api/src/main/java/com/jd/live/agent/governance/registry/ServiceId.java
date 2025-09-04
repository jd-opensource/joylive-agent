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
package com.jd.live.agent.governance.registry;

import com.jd.live.agent.governance.policy.service.ServiceName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import static com.jd.live.agent.core.Constants.DEFAULT_GROUP_BIPREDICATE;

@Getter
@Setter
public class ServiceId implements Serializable {

    protected String namespace;

    protected String service;

    protected String group;

    protected boolean interfaceMode;

    protected String uniqueName;

    public ServiceId() {
    }

    public ServiceId(String service) {
        this(null, service, null, false);
    }

    public ServiceId(String service, String group) {
        this(null, service, group, false);
    }

    public ServiceId(String service, String group, boolean interfaceMode) {
        this(null, service, group, interfaceMode);
    }

    public ServiceId(String namespace, String service, String group) {
        this(namespace, service, group, false);
    }

    public ServiceId(String namespace, String service, String group, boolean interfaceMode) {
        this.namespace = namespace;
        this.service = service;
        this.group = group;
        this.interfaceMode = interfaceMode;
    }

    public String getUniqueName() {
        if (uniqueName == null) {
            uniqueName = ServiceName.getUniqueName(namespace, service, group);
        }
        return uniqueName;
    }

    /**
     * Creates a new ServiceId with the specified service name and group.
     *
     * @param newService the new service name
     * @param newGroup   the new group name
     * @return a new ServiceId instance with the specified parameters
     */
    public ServiceId of(String newService, String newGroup) {
        return new ServiceId(namespace, newService, newGroup, interfaceMode);
    }

    /**
     * Checks if this service matches the target service using the same rules as {@link #match(ServiceId, ServiceId, String)}.
     *
     * @param target target service to match against (may be null)
     * @param defaultGroup default group name used when this service's group is empty (may be null)
     * @return true if services match (case-insensitive) and groups match according to rules
     */
    public boolean match(ServiceId target, String defaultGroup) {
        return match(this, target, defaultGroup);
    }

    /**
     * Determines if two service IDs match according to the specified matching rules.
     *
     * @param source the source service ID to match against (may be null)
     * @param target the target service ID to check (may be null)
     * @param defaultGroup the default group name to use when source group is empty (may be null)
     * @return true if the service IDs match according to the rules, false otherwise
     */
    public static boolean match(ServiceId source, ServiceId target, String defaultGroup) {
        String sourceService = source == null ? null : source.getService();
        String targetService = target == null ? null : target.getService();
        if (sourceService == null || !sourceService.equalsIgnoreCase(targetService)) {
            return false;
        }
        String sourceGroup = source == null ? null : source.getGroup();
        String targetGroup = target == null ? null : target.getGroup();
        if (DEFAULT_GROUP_BIPREDICATE.test(sourceGroup, defaultGroup)) {
            return DEFAULT_GROUP_BIPREDICATE.test(targetGroup, defaultGroup);
        }
        return sourceGroup.equalsIgnoreCase(targetGroup);
    }

}
