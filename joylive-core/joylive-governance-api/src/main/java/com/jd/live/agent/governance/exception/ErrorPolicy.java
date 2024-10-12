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
package com.jd.live.agent.governance.exception;

import java.util.Set;

/**
 * An interface representing an error policy.
 */
public interface ErrorPolicy {

    /**
     * Checks if the feature or functionality is enabled.
     *
     * @return true if the feature or functionality is enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * Checks if the specified error code is present in the list of error codes.
     *
     * @param errorCode the error code to check.
     * @return {@code true} if the error code is present, {@code false} otherwise.
     */
    boolean containsError(String errorCode);

    /**
     * Checks if the given class name is present in the list of exceptions.
     *
     * @param className The class name to check.
     * @return true if the class name is found in the list of exceptions, false otherwise.
     */
    boolean containsException(String className);

    /**
     * Checks if any of the given class names are present in the list of exceptions.
     *
     * @param classNames The set of class names to check.
     * @return true if any of the class names are found in the list of exceptions, false otherwise.
     */
    boolean containsException(Set<String> classNames);

    /**
     * Checks if any of the exception sources are present in the set of target exceptions.
     *
     * @param sources the set of exception sources to check.
     * @param targets the set of target exceptions to check against.
     * @return true if any of the exception sources are found in the set of target exceptions, false otherwise.
     */
    static boolean containsException(Set<String> sources, Set<String> targets) {
        if (targets == null || targets.isEmpty() || sources == null || sources.isEmpty()) {
            return false;
        }
        Set<String> lows = sources.size() < targets.size() ? sources : targets;
        Set<String> mores = sources.size() < targets.size() ? targets : sources;
        for (String low : lows) {
            if (mores.contains(low)) {
                return true;
            }
        }
        return false;
    }
}
