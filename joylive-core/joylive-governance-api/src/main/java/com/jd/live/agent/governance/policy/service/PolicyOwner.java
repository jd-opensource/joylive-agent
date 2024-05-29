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

import java.util.function.BiConsumer;

/**
 * Represents an abstract owner of a service policy, extending the capabilities of a PolicyId.
 */
@Getter
public abstract class PolicyOwner extends PolicyId {

    protected transient final Owner owners = new Owner();

    /**
     * Deletes the service policy using the specified merger and owner, and returns whether the owner list is not empty.
     *
     * @param merger The policy merger to handle the deletion logic.
     * @param owner  The owner of the service policy.
     * @return true if the owner list is not empty after deletion, false otherwise.
     */
    protected boolean onDelete(PolicyMerger merger, String owner) {
        owners.removeOwner(owner);
        own((p, o) -> {
            o.removeOwner(owner);
            merger.onDelete(p);
        });
        return owners.hasOwner();
    }

    /**
     * Adds the service policy using the specified merger and owner.
     *
     * @param merger The policy merger to handle the addition logic.
     * @param owner  The owner of the service policy.
     */
    protected void onAdd(PolicyMerger merger, String owner) {
        owners.addOwner(owner);
        own((p, o) -> {
            o.addOwner(owner);
            merger.onAdd(p);
        });
    }

    /**
     * Iterate policies and applies ownership logic using the provided consumer.
     *
     * @param consumer The consumer to apply ownership logic.
     */
    protected void own(BiConsumer<ServicePolicy, Owner> consumer) {

    }

}

