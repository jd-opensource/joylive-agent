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

import lombok.Getter;

import java.util.Collection;
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
    @Getter
    private Set<String> names;

    /**
     * Set of name prefixes to exclude (case-sensitive)
     */
    @Getter
    private Set<String> prefixes;

    private boolean nullable;

    public Inclusion() {
    }

    public Inclusion(boolean nullable) {
        this.nullable = nullable;
    }

    public Inclusion(Set<String> names, Set<String> prefixes) {
        this.names = names;
        this.prefixes = prefixes;
    }

    public Inclusion(Set<String> names, Set<String> prefixes, boolean nullable) {
        this.names = names;
        this.prefixes = prefixes;
        this.nullable = nullable;
    }

    /**
     * Adds an exact name to the inclusion list
     *
     * @param name the exact class name to exclude (must not be null)
     */
    public void addName(String name) {
        if (name == null) {
            return;
        }
        if (names == null) {
            names = new HashSet<>(8);
        }
        names.add(name);
    }

    /**
     * Adds multiple names to the inclusion list.
     *
     * @param names collection of names to add (ignored if null)
     */
    public void addNames(Collection<String> names) {
        if (names == null) {
            return;
        }
        if (this.names == null) {
            this.names = new HashSet<>(8);
        }
        this.names.addAll(names);
    }

    /**
     * Adds a name prefix to the inclusion list
     *
     * @param prefix the class name prefix to include (must not be null)
     */
    public void addPrefix(String prefix) {
        if (prefixes == null) {
            prefixes = new HashSet<>(8);
        }
        prefixes.add(prefix);
    }

    /**
     * Adds multiple prefixes to the inclusion list.
     *
     * @param prefixes collection of prefixes to add (ignored if null)
     */
    public void addPrefixes(Collection<String> prefixes) {
        if (prefixes == null) {
            return;
        }
        if (this.prefixes == null) {
            this.prefixes = new HashSet<>(8);
        }
        this.prefixes.addAll(prefixes);
    }

    @Override
    public boolean test(String name) {
        return test(names, prefixes, nullable, name);
    }

    /**
     * Tests whether a given name matches any exclusion pattern in the provided sets.
     *
     * @param names    the set of exact names to match against.
     * @param prefixes the set of prefixes to match against.
     * @param nullable determines the return value when {@code name} is null or empty:
     *                 {@code true} to accept null or empty names, {@code false} to reject them
     * @param name     the name to test (may be {@code null})
     * @return {@code true} if the name matches any exclusion rule, {@code false} otherwise
     */
    public static boolean test(Set<String> names, Set<String> prefixes, boolean nullable, String name) {
        if (name == null || name.isEmpty()) {
            return nullable;
        } else if (names != null && names.contains(name)) {
            return true;
        } else if (prefixes != null) {
            for (String prefix : prefixes) {
                if (name.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }
}
