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
import lombok.Setter;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
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
    @Setter
    private Set<String> names;

    /**
     * Set of name prefixes to exclude (case-sensitive)
     */
    @Getter
    @Setter
    private Set<String> prefixes;

    private final boolean nullable;

    private final Predicate<String> prefixPredicate;

    public Inclusion() {
        this(null, null, false, null);
    }

    public Inclusion(Set<String> names, Set<String> prefixes) {
        this(names, prefixes, false, null);
    }

    public Inclusion(Set<String> names, Set<String> prefixes, boolean nullable) {
        this(names, prefixes, nullable, null);
    }

    public Inclusion(Set<String> names, Set<String> prefixes, boolean nullable, PrefixPredicateFactory factory) {
        this.names = names;
        this.prefixes = prefixes;
        this.nullable = nullable;
        this.prefixPredicate = factory != null ? factory.create(prefixes) : DefaultPrefixPredicateFactory.INSTANCE.create(prefixes);
    }

    @Override
    public boolean test(String name) {
        return include(name, null) != InclusionType.EXCLUDE;
    }

    /**
     * Tests if a name matches the inclusion rules with prefix transformation.
     *
     * @param name            the name to test
     * @param prefixConverter optional name transformer for prefix matching
     * @return true if name is included (not EXCLUDE)
     */
    public boolean test(String name, Function<String, String> prefixConverter) {
        return include(name, prefixConverter) != InclusionType.EXCLUDE;
    }

    /**
     * Determines the inclusion type for a name.
     *
     * @param name the name to check
     * @return the inclusion classification (INCLUDE/EXCLUDE/NEUTRAL)
     */
    public InclusionType include(String name) {
        return include(name, null);
    }

    /**
     * Determines how to include a name based on exact matches and prefix rules.
     *
     * @param name            the name to evaluate (null/empty returns EXCLUDE)
     * @param prefixConverter optional name transformer for prefix matching
     * @return the inclusion decision based on matching rules
     * @see InclusionType for possible return values
     */
    private InclusionType include(String name, Function<String, String> prefixConverter) {
        if (name == null || name.isEmpty()) {
            return InclusionType.EXCLUDE;
        } else {
            boolean nameEmpty = names == null || names.isEmpty();
            boolean prefixEmpty = prefixes == null || prefixes.isEmpty();
            if (!nameEmpty && names.contains(name)) {
                return InclusionType.INCLUDE_EXACTLY;
            } else if (!prefixEmpty && prefixPredicate != null) {
                String prefix = prefixConverter == null ? name : prefixConverter.apply(name);
                if (prefixPredicate.test(prefix)) {
                    return InclusionType.INCLUDE_OTHER;
                }
            }
            return nullable && nameEmpty && prefixEmpty ? InclusionType.INCLUDE_EMPTY : InclusionType.EXCLUDE;
        }
    }

    /**
     * Creates a new builder for constructing inclusion rules.
     *
     * @return a fresh builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder with nullability and prefix matching configuration.
     *
     * @param nullable whether null values should be included by default
     * @param factory  the strategy for creating prefix matchers (null allowed)
     * @return a new builder instance with specified configurations
     */
    public static Builder builder(boolean nullable, PrefixPredicateFactory factory) {
        return new Builder().nullable(nullable).factory(factory);
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

    /**
     * Factory for creating prefix matcher instances.
     */
    // TODO use a trie to improve performance
    public interface PrefixPredicateFactory {
        /**
         * Creates a new matcher for the given prefix set.
         *
         * @param prefixes the prefixes to match against
         * @return a configured prefix matcher instance
         */
        Predicate<String> create(Set<String> prefixes);
    }

    /**
     * Default implementation that creates predicates checking string prefixes.
     */
    public static class DefaultPrefixPredicateFactory implements PrefixPredicateFactory {

        public static final PrefixPredicateFactory INSTANCE = new DefaultPrefixPredicateFactory();

        @Override
        public Predicate<String> create(Set<String> prefixes) {
            return s -> {
                if (prefixes != null && !prefixes.isEmpty()) {
                    for (String prefix : prefixes) {
                        if (s.startsWith(prefix)) {
                            return true;
                        }
                    }
                }
                return false;
            };
        }
    }

    /**
     * Factory that creates predicates checking exact string matches (not prefixes).
     */
    public static class ContainsPredicateFactory implements PrefixPredicateFactory {

        public static final PrefixPredicateFactory INSTANCE = new ContainsPredicateFactory();

        @Override
        public Predicate<String> create(Set<String> prefixes) {
            return s -> {
                if (s != null && !s.isEmpty() && prefixes != null && !prefixes.isEmpty()) {
                    return prefixes.contains(s);
                }
                return false;
            };
        }
    }


    /**
     * Builder for constructing {@link Inclusion} rules with class name patterns.
     */
    public static class Builder {
        private Set<String> names;
        private Set<String> prefixes;
        private boolean nullable;
        private PrefixPredicateFactory factory;

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder factory(PrefixPredicateFactory factory) {
            this.factory = factory;
            return this;
        }

        /**
         * Adds an exact name to the inclusion list
         *
         * @param name the exact class name to exclude (must not be null)
         * @return this builder for method chaining
         */
        public Builder addName(String name) {
            if (name != null && !name.isEmpty()) {
                if (names == null) {
                    names = new HashSet<>(8);
                }
                names.add(name);
            }
            return this;
        }

        /**
         * Adds multiple elements to the builder by applying a consumer to each.
         *
         * @param <T>        the type of elements to add
         * @param collection elements to process (ignored if null)
         * @param consumer   operation to perform for each element
         * @return this builder for chaining
         */
        public <T> Builder add(Collection<T> collection, BiConsumer<Builder, T> consumer) {
            if (collection != null) {
                collection.forEach(t -> consumer.accept(this, t));
            }
            return this;
        }

        /**
         * Adds multiple names to the inclusion list.
         *
         * @param names collection of names to add (ignored if null)
         * @return this builder for method chaining
         */
        public Builder addNames(Collection<String> names) {
            if (names != null) {
                if (this.names == null) {
                    this.names = new HashSet<>(names);
                } else {
                    this.names.addAll(names);
                }
            }
            return this;
        }

        /**
         * Adds multiple names to the inclusion list.
         *
         * @param names array of names to add (ignored if null)
         * @return this builder for method chaining
         */
        public Builder addNames(String[] names) {
            return names == null ? this : addNames(Arrays.asList(names));
        }

        /**
         * Adds a name prefix to the inclusion list
         *
         * @param prefix the class name prefix to include (must not be null)
         * @return this builder for method chaining
         */
        public Builder addPrefix(String prefix) {
            if (prefix != null && !prefix.isEmpty()) {
                if (prefixes == null) {
                    prefixes = new HashSet<>(8);
                }
                prefixes.add(prefix);
            }
            return this;
        }

        /**
         * Adds multiple prefixes to the inclusion list.
         *
         * @param prefixes collection of prefixes to add (ignored if null)
         * @return this builder for method chaining
         */
        public Builder addPrefixes(Collection<String> prefixes) {
            if (prefixes != null) {
                if (this.prefixes == null) {
                    this.prefixes = new HashSet<>(prefixes);
                } else {
                    this.prefixes.addAll(prefixes);
                }
            }
            return this;
        }

        /**
         * Adds multiple prefixes to the inclusion list.
         *
         * @param prefixes array of prefixes to add (ignored if null)
         * @return this builder for method chaining
         */
        public Builder addPrefixes(String[] prefixes) {
            return prefixes == null ? this : addPrefixes(Arrays.asList(prefixes));
        }

        /**
         * Adds multiple class name prefixes with optional transformation.
         *
         * @param prefixes  array of class name prefixes (ignored if null)
         * @param converter function to transform each prefix (optional, may be null)
         * @return this builder for fluent chaining
         */
        public Builder addPrefixes(String[] prefixes, Function<String, String> converter) {
            if (prefixes == null) {
                return this;
            }
            if (converter == null) {
                return addPrefixes(Arrays.asList(prefixes));
            }
            for (String prefix : prefixes) {
                addPrefix(converter.apply(prefix));
            }
            return this;
        }

        /**
         * Processes and adds a class name based on its suffix pattern.
         * <p>
         * Handles special cases for class names ending with:
         * <ul>
         *   <li>'/', '.', or '$' - treats as a prefix match</li>
         *   <li>'*' - treats as a wildcard prefix match (excluding the '*')</li>
         *   <li>Other characters - treats as exact match</li>
         * </ul>
         *
         * @param className the class name to process (null or empty values are ignored)
         * @return this builder for method chaining
         */
        public Builder addClassName(String className) {
            if (className == null || className.isEmpty()) {
                return this;
            }
            className = className.trim();
            int length = className.length();
            if (length > 0) {
                char ch = className.charAt(length - 1);
                switch (ch) {
                    case '/':
                    case '.':
                    case '$':
                        if (length > 1) {
                            addPrefix(className);
                        }
                        break;
                    case '*':
                        if (length > 1) {
                            addPrefix(className.substring(0, length - 1));
                        }
                        break;
                    default:
                        addName(className);
                }
            }
            return this;
        }

        public Inclusion build() {
            return new Inclusion(names, prefixes, nullable, factory);
        }
    }


}
