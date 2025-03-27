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

/**
 * Represents the name of a service, including its namespace.
 */
public interface ServiceName {

    /**
     * Returns the namespace of the service.
     *
     * @return The namespace of the service.
     */
    String getNamespace();

    /**
     * Returns the name of the service.
     *
     * @return The name of the service.
     */
    String getName();

    /**
     * Gets unique name using current namespace and name.
     */
    default String getUniqueName() {
        return getUniqueName(getNamespace(), getName(), null);
    }

    /**
     * Generates a unique identifier from namespace and name.
     * Delegates to {@link #getUniqueName(String, String, String)} without group.
     *
     * @param name      Base name (non-null, non-empty)
     * @param namespace Optional namespace (null/empty means default)
     * @return Formatted unique name
     * @see #getUniqueName(String, String, String)
     */
    static String getUniqueName(String namespace, String name) {
        return getUniqueName(namespace, name, null);
    }

    /**
     * Generates hierarchical unique identifier with format rules:
     * - No group/namespace → name
     * - Namespace only → name@@namespace
     * - Group only → name@group
     * - Both → name@group@namespace
     *
     * @param namespace Optional namespace (null/empty means default)
     * @param group     Optional group name (null/empty skips)
     * @param name      Base name (non-null, non-empty)
     * @return Formatted unique name
     */
    static String getUniqueName(String namespace, String name, String group) {
        if (group == null || group.isEmpty()) {
            return namespace == null || namespace.isEmpty() ? name : name + "@@" + namespace;
        } else if (namespace == null || namespace.isEmpty()) {
            return name + "@" + group;
        }
        return name + "@" + group + "@" + namespace;
    }
}
