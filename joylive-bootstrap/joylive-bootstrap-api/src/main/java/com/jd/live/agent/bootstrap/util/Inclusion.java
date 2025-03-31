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
package com.jd.live.agent.bootstrap.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Helper class for managing exclusion rules for boot classes.
 */
public class Inclusion implements Predicate<String> {

    /**
     * Set of exact names to exclude (case-sensitive)
     */
    private final Set<String> names;

    /**
     * Set of name prefixes to exclude (case-sensitive)
     */
    private final Set<String> prefixes;

    private final boolean nullable;

    public Inclusion() {
        this(null, null, false);
    }

    public Inclusion(boolean nullable) {
        this(null, null, nullable);
    }

    public Inclusion(Set<String> names, Set<String> prefixes, boolean nullable) {
        this.names = names == null ? new HashSet<>(20) : names;
        this.prefixes = prefixes == null ? new HashSet<>(10) : prefixes;
        this.nullable = nullable;
    }

    /**
     * Adds an exact name to the exclusion list
     *
     * @param name the exact class name to exclude (must not be null)
     */
    public void addName(String name) {
        names.add(name);
    }

    /**
     * Adds a name prefix to the exclusion list
     *
     * @param prefix the class name prefix to exclude (must not be null)
     */
    public void addPrefix(String prefix) {
        prefixes.add(prefix);
    }

    @Override
    public boolean test(String name) {
        if (name == null) {
            return !nullable;
        } else if (names.contains(name)) {
            return true;
        }
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
