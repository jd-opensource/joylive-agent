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
package com.jd.live.agent.governance.policy.service;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

/**
 * ServiceMethod
 */
@Setter
@Getter
public class ServiceMethod extends ServicePolicyOwner {

    private String name;

    public ServiceMethod() {
    }

    public ServiceMethod(String name) {
        this.name = name;
    }

    public ServiceMethod(String name, ServicePolicy servicePolicy) {
        this.name = name;
        this.servicePolicy = servicePolicy;
    }

    protected ServiceMethod copy() {
        ServiceMethod result = new ServiceMethod();
        result.id = id;
        result.uri = uri;
        result.tags = tags == null ? null : new HashMap<>(tags);
        result.servicePolicy = servicePolicy == null ? null : servicePolicy.clone();
        result.name = name;
        result.owners.addOwner(owners);
        return result;
    }
}
