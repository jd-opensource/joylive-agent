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

    protected String uniqueName;

    public ServiceId() {
    }

    public ServiceId(String service) {
        this(null, null, service, null);
    }

    public ServiceId(String service, String group) {
        this(null, null, service, group);
    }

    public ServiceId(String namespace, String service, String group) {
        this(null, namespace, service, group);
    }

    public ServiceId(String id, String namespace, String service, String group) {
        this.id = id;
        this.namespace = namespace;
        this.service = service;
        this.group = group;
    }

    public String getUniqueName() {
        if (uniqueName == null) {
            uniqueName = ServiceName.getUniqueName(namespace, service, group);
        }
        return uniqueName;
    }
}
