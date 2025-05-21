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

@Getter
@Setter
public class ServiceId implements Serializable {

    protected String id;

    protected String namespace;

    protected String service;

    protected String group;

    protected boolean interfaceMode;

    protected String uniqueName;

    public ServiceId() {
    }

    public ServiceId(String service) {
        this(null, null, service, null, false);
    }

    public ServiceId(String service, String group) {
        this(null, null, service, group, false);
    }

    public ServiceId(String service, String group, boolean interfaceMode) {
        this(null, null, service, group, interfaceMode);
    }

    public ServiceId(String namespace, String service, String group) {
        this(null, namespace, service, group, false);
    }

    public ServiceId(String id, String namespace, String service, String group) {
        this(id, namespace, service, group, false);
    }

    public ServiceId(String id, String namespace, String service, String group, boolean interfaceMode) {
        this.id = id;
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
     * Checks if ServiceId matches given service and group.
     *
     * @param service      service name to match
     * @param group        group name to match
     * @param defaultGroup default group name for fallback matching
     * @return true if both service and group match
     */
    public boolean match(String service, String group, String defaultGroup) {
        return match(this.service, this.group, service, group, defaultGroup);
    }

    /**
     * Determines if a source service/group matches a target service/group with default group fallback.
     * <p>
     * Matching rules:
     * <ul>
     *   <li>Service names must match (case-insensitive)</li>
     *   <li>If source group is null or empty, matches:
     *     <ul>
     *       <li>Null/empty target group, OR</li>
     *       <li>Target group matching default group (case-insensitive)</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise requires exact group match (case-insensitive)</li>
     * </ul>
     *
     * @param sourceService the source service name to match
     * @param sourceGroup   the source group name to match (may be null or empty)
     * @param targetService the target service name to compare against
     * @param targetGroup   the target group name to compare against
     * @param defaultGroup  the default group name for fallback matching
     * @return true if services and groups match according to rules, false otherwise
     */
    public static boolean match(String sourceService, String sourceGroup, String targetService, String targetGroup, String defaultGroup) {
        if (sourceService == null || !sourceService.equalsIgnoreCase(targetService)) {
            return false;
        }
        if (sourceGroup == null || sourceGroup.isEmpty()) {
            return targetGroup == null || targetGroup.isEmpty() || targetGroup.equalsIgnoreCase(defaultGroup);
        }
        return sourceGroup.equalsIgnoreCase(targetGroup);
    }
}
