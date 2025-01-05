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
 * Represents a version of a policy. This interface provides a method to retrieve
 * the version number of the policy.
 *
 * @since 1.6.0
 */
public interface PolicyVersion {

    /**
     * Returns the version number of the policy.
     *
     * @return the version number as a long
     */
    long getVersion();

    /**
     * Checks if the current policy version is older than the specified policy version.
     *
     * @param other the policy version to compare against
     * @return {@code true} if the current policy version is older than the specified policy version,
     * or if the specified policy version is not null and the current version is less than or equal to it;
     * {@code false} otherwise
     */
    default boolean isOlderThan(PolicyVersion other) {
        return other != null && getVersion() <= other.getVersion();
    }
}
