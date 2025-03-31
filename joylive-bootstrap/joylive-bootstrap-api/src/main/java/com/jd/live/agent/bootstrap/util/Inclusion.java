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
import java.util.function.Function;
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
        // TODO use a trie to improve performance
        return execute(names, prefixes, nullable, name, null, null) != InclusionType.EXCLUDE;
    }

    /**
     * Tests whether a given name matches any inclusion pattern in the provided sets.
     *
     * @param names    the set of exact names to match against.
     * @param prefixes the set of prefixes to match against.
     * @param nullable whether empty sets should match any name
     * @param name     the name to test (may be {@code null})
     * @return {@code true} if the name matches any exclusion rule, {@code false} otherwise
     */
    public static boolean test(Set<String> names, Set<String> prefixes, boolean nullable, String name) {
        return execute(names, prefixes, nullable, name, null, null) != InclusionType.EXCLUDE;
    }

    /**
     * Checks if a name matches any inclusion rule in the given sets.
     *
     * @param names      set of exact names to match
     * @param prefixes   set of prefixes to match
     * @param nullable   whether empty sets should match any name
     * @param name       the name to test
     * @param prefixFunc optional function to transform name before prefix check
     * @return true if name matches any rule, false otherwise
     */
    public static boolean test(Set<String> names,
                               Set<String> prefixes,
                               boolean nullable,
                               String name,
                               Function<String, String> prefixFunc) {
        return execute(names, prefixes, nullable, name, null, prefixFunc) != InclusionType.EXCLUDE;
    }


    /**
     * Determines how a name should be included based on matching rules.
     *
     * @param names      exact names to match (null/empty means no exact matches)
     * @param prefixes   prefixes to match (null/empty means no prefix matches)
     * @param nullable   whether to include when both sets are empty
     * @param name       name to check (null/empty always returns EXCLUDE)
     * @param prefixFunc optional name transformer for prefix matching
     * @return inclusion type indicating match result (never null)
     */
    public static InclusionType execute(Set<String> names,
                                        Set<String> prefixes,
                                        boolean nullable,
                                        String name,
                                        Function<String, String> prefixFunc) {
        return execute(names, prefixes, nullable, name, null, prefixFunc);
    }

    /**
     * Determines the inclusion type of a name based on matching rules against name and other sets.
     *
     * @param names          set of exact names to match
     * @param others         set of other patterns to match against
     * @param nullable       whether to include when both sets are empty
     * @param name           name to check
     * @param otherPredicate predicate to test other patterns
     * @param otherNameFunc  function to transform name before other matching
     * @return inclusion type indicating the match result
     * @see InclusionType
     */
    public static InclusionType execute(Set<String> names,
                                        Set<String> others,
                                        boolean nullable,
                                        String name,
                                        Predicate<String> otherPredicate,
                                        Function<String, String> otherNameFunc) {
        if (name == null || name.isEmpty()) {
            return InclusionType.EXCLUDE;
        } else {
            boolean nameEmpty = names == null || names.isEmpty();
            boolean otherEmpty = others == null || others.isEmpty();
            if (!nameEmpty && names.contains(name)) {
                return InclusionType.INCLUDE_EXACTLY;
            } else if (!otherEmpty) {
                String otherName = otherNameFunc == null ? name : otherNameFunc.apply(name);
                Predicate<String> predicate = otherPredicate == null ? otherName::startsWith : otherPredicate;
                for (String other : others) {
                    if (predicate.test(other)) {
                        return InclusionType.INCLUDE_OTHER;
                    }
                }
            }
            return nullable && nameEmpty && otherEmpty ? InclusionType.INCLUDE_EMPTY : InclusionType.EXCLUDE;
        }
    }

    /**
     * Enum representing different inclusion match types.
     */
    public enum InclusionType {
        /**
         * Name exactly matches an entry in the names set
         */
        INCLUDE_EXACTLY,

        /**
         * Name matches an entry in the others set according to the predicate
         */
        INCLUDE_OTHER,

        /**
         * Included because both sets were empty and nullable was true
         */
        INCLUDE_EMPTY,

        /**
         * Name didn't match any inclusion criteria
         */
        EXCLUDE
    }
}
