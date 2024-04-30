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
package com.jd.live.agent.core.util.version;

/**
 * Interface for matching version patterns.
 * This interface defines methods for matching versions, one accepts a version as a string,
 * and the other accepts a {@link Version} object.
 * The default implementation for the string version ensures that a non-null and non-empty
 * version string can be transformed into a {@link Version} object for matching.
 *
 * @author Zhiguo.Chen
 * @since 2024-01-18
 */
public interface VersionMatcher {

    /**
     * Matches a version given as a string.
     * This method checks if the provided version string is non-null and not empty,
     * then transforms it into a {@link Version} object for further matching.
     *
     * @param version the version string to be matched.
     * @return true if the version matches; false otherwise.
     */
    default boolean match(String version) {
        return version != null && !version.isEmpty() && match(new Version(version));
    }

    /**
     * Matches a version given as a {@link Version} object.
     *
     * @param version the {@link Version} object to be matched.
     * @return true if the version matches; false otherwise.
     */
    boolean match(Version version);
}

