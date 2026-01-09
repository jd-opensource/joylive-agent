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
package com.jd.live.agent.core.util.trie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A Trie-based implementation for matching and storing paths.
 *
 * @param <T> The type of path objects stored in the Trie, which must extend the {@code Path} class.
 */
public class PathMatcherTrie<T extends Path> implements PathTrie<T> {

    private final Supplier<Character> delimiter;

    private final Supplier<List<T>> supplier;

    private volatile Cache<T> cache;

    /**
     * Constructs a {@code PathMatcherTrie} with the default delimiter.
     *
     * @param supplier A supplier providing a list of paths to be added to the Trie.
     */
    public PathMatcherTrie(Supplier<List<T>> supplier) {
        this(PathType.URL.getDelimiter(), supplier);
    }

    /**
     * Constructs a {@code PathMatcherTrie} with the specified delimiter.
     *
     * @param delimiter The delimiter used to separate path segments.
     * @param supplier  A supplier providing a list of paths to be added to the Trie.
     */
    public PathMatcherTrie(char delimiter, Supplier<List<T>> supplier) {
        this(() -> delimiter, supplier);
    }

    /**
     * Constructs a {@code PathMatcherTrie} with the specified delimiter supplier.
     *
     * @param delimiter A supplier providing the delimiter used to separate path segments.
     * @param supplier  A supplier providing a list of paths to be added to the Trie.
     */
    public PathMatcherTrie(Supplier<Character> delimiter, Supplier<List<T>> supplier) {
        this.delimiter = delimiter;
        this.supplier = supplier;
    }

    @Override
    public T match(String path, PathMatchType type) {
        PathMatcher.MatchResult<T> result = getCache().match(path);
        if (result == null) {
            return null;
        } else if (type == null || type == PathMatchType.PREFIX) {
            return result.getValue();
        } else {
            return result.getType() == PathMatchType.EQUAL ? result.getValue() : null;
        }
    }

    @Override
    public T get(String path) {
        return path == null ? null : getCache().getPath(path);
    }

    /**
     * Clears all the paths stored in the Trie.
     */
    @Override
    public void clear() {
        cache = null;
    }

    /**
     * Retrieves the cache of paths, initializing it if necessary.
     *
     * @return The cache of paths.
     */
    private Cache<T> getCache() {
        Cache<T> result = cache;
        if (result != null) {
            return result;
        }
        synchronized (this) {
            result = cache;
            if (result == null) {
                result = new Cache<>(supplier, delimiter.get());
                cache = result;
            }
        }
        return result;
    }

    private static class Cache<T extends Path> {

        private final Map<String, T> paths;

        private final PathMatcher<T> matcher;

        Cache(Supplier<List<T>> supplier, Character delimiter) {
            paths = createPaths(supplier);
            matcher = createMatcher(supplier, delimiter);
        }

        public T getPath(String path) {
            return path == null ? null : paths.get(path);
        }

        public PathMatcher.MatchResult<T> match(String path) {
            return path == null ? null : matcher.match(path);
        }

        private Map<String, T> createPaths(Supplier<List<T>> supplier) {
            Map<String, T> result = new HashMap<>();
            if (supplier != null) {
                List<T> paths = supplier.get();
                if (paths != null) {
                    for (T path : paths) {
                        result.put(path.getPath(), path);
                    }
                }
            }
            return result;
        }

        private PathMatcher<T> createMatcher(Supplier<List<T>> supplier, Character delimiter) {
            PathMatcher<T> result = new PathMatcher<>(delimiter);
            if (supplier != null) {
                List<T> paths = supplier.get();
                if (paths != null) {
                    for (T path : paths) {
                        result.addPath(path.getPath(), path);
                    }
                }
            }
            return result;
        }
    }
}


