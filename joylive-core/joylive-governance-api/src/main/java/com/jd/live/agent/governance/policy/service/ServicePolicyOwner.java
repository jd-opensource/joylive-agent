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

import java.util.function.BiConsumer;

/**
 * Represents an abstract owner of a service policy, extending the capabilities of a PolicyId.
 */
@Setter
@Getter
public class ServicePolicyOwner extends PolicyOwner {

    protected ServicePolicy servicePolicy;

    /**
     * Supplements the current service policy with information from the provided source.
     *
     * @param source The service policy to supplement the current policy with. Can be null.
     */
    protected void supplement(ServicePolicy source) {
        if (source != null && servicePolicy == null) {
            servicePolicy = new ServicePolicy();
        }
        if (servicePolicy != null) {
            servicePolicy.supplement(() -> uri, supplementTag());
            servicePolicy.supplement(source);
        }
    }

    /**
     * Updates the service policy with information from the provided source using the specified merger and owner.
     *
     * @param source The new service policy information to update with.
     * @param merger The policy merger to handle the merging logic.
     * @param owner The owner of the service policy.
     */
    protected void onUpdate(ServicePolicy source, PolicyMerger merger, String owner) {
        owners.addOwner(owner);
        if (servicePolicy == null) {
            servicePolicy = source;
            merger.onAdd(servicePolicy);
        } else {
            merger.onUpdate(servicePolicy, source);
        }
    }

    @Override
    protected void own(BiConsumer<ServicePolicy, Owner> consumer) {
        if (consumer != null) {
            consumer.accept(servicePolicy, owners);
        }
    }

    /**
     * Caches the current service policy.
     */
    protected void cache() {
        if (servicePolicy != null) {
            servicePolicy.cache();
        }
    }
}

