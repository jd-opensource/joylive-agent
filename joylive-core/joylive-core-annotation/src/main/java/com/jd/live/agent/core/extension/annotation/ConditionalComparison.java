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
package com.jd.live.agent.core.extension.annotation;

import java.util.Objects;

/**
 * Represents the types of conditional comparisons.
 * Each enum value implements a specific comparison logic.
 */
public enum ConditionalComparison {
    /**
     * Indicates equality comparison.
     * Returns true if the source and target strings are equal.
     */
    EQUAL {
        @Override
        public boolean match(String source, String target) {
            return Objects.equals(source, target);
        }
    },

    /**
     * Indicates inequality comparison.
     * Returns true if the source and target strings are not equal.
     */
    NOT_EQUAL {
        @Override
        public boolean match(String source, String target) {
            return !Objects.equals(source, target);
        }
    };

    /**
     * Compares the source and target strings based on the comparison type.
     *
     * @param source The source string to compare.
     * @param target The target string to compare.
     * @return true if the comparison condition is met, false otherwise.
     */
    public abstract boolean match(String source, String target);
}