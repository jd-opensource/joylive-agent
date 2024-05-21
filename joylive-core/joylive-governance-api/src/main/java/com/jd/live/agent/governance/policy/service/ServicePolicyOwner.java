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

import com.jd.live.agent.governance.policy.PolicyId;
import lombok.Getter;
import lombok.Setter;

import java.util.function.BiConsumer;

/**
 * Represents an abstract owner of a service policy, extending the capabilities of a PolicyId.
 */
@Getter
public abstract class ServicePolicyOwner extends PolicyId {

    @Setter
    protected ServicePolicy servicePolicy;

    protected transient final Owner owners = new Owner();

    /**
     * Supplements the current service policy with information from the provided source.\
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
     * Merges the current service policy with the provided source policy using a specified consumer.
     *
     * @param source   The service policy to merge with the current policy. Can be null.
     * @param consumer A {@code BiConsumer} that defines how to merge the current policy with the source. Can be null.
     */
    protected void merge(ServicePolicy source, BiConsumer<ServicePolicy, ServicePolicy> consumer) {
        if (source != null) {
            if (servicePolicy == null) {
                servicePolicy = source;
            } else if (consumer != null) {
                consumer.accept(servicePolicy, source);
            }
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
