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
package com.jd.live.agent.governance.policy;

/**
 * The {@code PolicyInherit} interface defines a method for supplementing an instance with
 * properties or settings from a source object. This interface is used to implement policies that
 * can inherit or augment their behavior based on another instance's state.
 *
 * @param <T> the type of the source object from which properties or settings are supplemented
 */
public interface PolicyInherit<T> {

    /**
     * Supplements the policy instance with properties or settings from the specified source object.
     * This method allows the policy to inherit or update its behavior based on the state of the source.
     *
     * @param source the source object from which to supplement properties or settings
     */
    void supplement(T source);

    /**
     * The {@code PolicyInheritWithId} interface extends the functionality of the {@code InheritablePolicy}
     * interface by adding the capability to set a unique identifier for policy instances. This interface is
     * particularly useful for policies that need to be identifiable and distinguishable from one another.
     *
     * @param <T> the type of the source object which provides properties or settings
     *            that can be inherited or supplemented into this policy.
     */
    interface PolicyInheritWithId<T> extends PolicyInherit<T> {

        /**
         * Assigns an identifier to this policy. The identifier is typically used to reference the
         * policy in a data store or to differentiate it from other policies that may have similar settings.
         *
         * @param id the unique identifier to be assigned to the policy; must not be null.
         * @throws IllegalArgumentException if the provided id is null.
         */
        void setId(Long id);
    }

    /**
     * The {@code PolicyInheritWithIdGen} interface combines the functionalities of the
     * {@code InheritablePolicy} and {@code IdGenerator} interfaces. This interface allows for the
     * creation of policies that can be inherited and also have the capability to generate unique
     * identifiers for supplementing information, such as URLs, with an ID.
     *
     * @param <T> the type of the policy that is being extended with ID generation capabilities.
     */
    interface PolicyInheritWithIdGen<T> extends PolicyInherit<T>, PolicyIdGen {

    }

}

