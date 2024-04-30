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
package com.jd.live.agent.core.extension;

import java.util.Comparator;

/**
 * ExtensionDesc is an interface that provides metadata and access to an extension's description.
 * It includes information such as the class loader, the extensible type, the name of the extension,
 * the provider, the order, whether it is a singleton, and the target object of the extension.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface ExtensionDesc<T> {

    /**
     * Retrieves the ClassLoader associated with this extension description.
     *
     * @return the ClassLoader for this extension.
     */
    ClassLoader getClassLoader();

    /**
     * Gets the extensible type that this extension is associated with.
     *
     * @return the extensible type.
     */
    Name<T> getExtensible();

    /**
     * Retrieves the name of this extension.
     *
     * @return the name of the extension.
     */
    Name<T> getName();

    /**
     * Gets the provider of this extension.
     *
     * @return the provider of the extension.
     */
    String getProvider();

    /**
     * Retrieves the order of this extension.
     *
     * @return the order value.
     */
    int getOrder();

    /**
     * Checks if this extension is a singleton.
     *
     * @return true if the extension is a singleton, false otherwise.
     */
    boolean isSingleton();

    /**
     * Gets the target object of this extension.
     *
     * @return the target object.
     */
    T getTarget();

    /**
     * AscendingComparator is a utility class that compares two ExtensionDesc objects based on their order.
     * It implements the Comparator interface and provides a static instance for convenience.
     */
    class AscendingComparator implements Comparator<ExtensionDesc<?>> {

        /**
         * Singleton instance of the AscendingComparator.
         */
        public static final AscendingComparator INSTANCE = new AscendingComparator();

        @Override
        public int compare(ExtensionDesc<?> o1, ExtensionDesc<?> o2) {
            return o1.getOrder() - o2.getOrder();
        }
    }
}

