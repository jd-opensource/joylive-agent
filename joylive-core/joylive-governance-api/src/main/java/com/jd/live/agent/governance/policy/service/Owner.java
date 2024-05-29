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

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an entity responsible for managing a set of owners in synchronization. It provides functionality
 * to add, remove, and check the presence of owners within a collection.
 */
public class Owner {

    /**
     * A set of strings representing the unique identifiers for each owner.
     */
    private Set<String> owners;

    /**
     * Adds a new owner to the set of owners if the owner is not null. If the set of owners
     * has not been initialized yet, it initializes the set before adding the new owner.
     *
     * @param owner The identifier of the owner to be added. Must not be null.
     */
    public void addOwner(String owner) {
        if (owner != null) {
            if (owners == null) {
                owners = new HashSet<>();
            }
            owners.add(owner);
        }
    }

    public void addOwner(Owner owner) {
        if (owner != null && owner.hasOwner()) {
            if (owners == null) {
                owners = new HashSet<>();
            }
            owners.addAll(owner.owners);
        }
    }

    /**
     * Removes an owner from the set of owners if the owner is not null and the set of owners
     * is not empty. This method has no effect if the owner is not present in the set.
     *
     * @param owner The identifier of the owner to be removed. Must not be null.
     */
    public void removeOwner(String owner) {
        if (owner != null && owners != null) {
            owners.remove(owner);
        }
    }

    /**
     * Resets the set of owners and adds the specified owner as the sole owner. If the owner
     * parameter is not null, this method initializes a new set of owners and adds the specified
     * owner to it. This method effectively clears any previous owner information.
     *
     * @param owner The identifier of the owner to be set as the sole owner. Must not be null.
     */
    public void own(String owner) {
        if (owner != null) {
            owners = new HashSet<>();
            owners.add(owner);
        }
    }

    /**
     * Checks whether the set of owners is empty or not initialized.
     *
     * @return {@code true} if the set of owners is null or empty, {@code false} otherwise.
     */
    public boolean hasOwner() {
        return owners != null && !owners.isEmpty();
    }

}

