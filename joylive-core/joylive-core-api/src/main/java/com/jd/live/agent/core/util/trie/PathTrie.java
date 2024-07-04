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

/**
 * Interface representing a Trie data structure for storing and retrieving paths.
 *
 * @param <T> The type of path objects stored in the Trie, which must extend the {@code Path} class.
 */
public interface PathTrie<T extends Path> {

    /**
     * Matches a given path against the Trie and returns the corresponding path object.
     *
     * @param path The path to be matched.
     * @param type The type of path matching to be performed.
     * @return The matched path object of type {@code T}, or {@code null} if no match is found.
     */
    T match(String path, PathMatchType type);

    /**
     * Retrieves the path object associated with the given path.
     *
     * @param path The path whose associated path object is to be returned.
     * @return The path object associated with the specified path, or {@code null} if no such path is found.
     */
    T get(String path);

    /**
     * Clears all the paths stored in the Trie.
     */
    void clear();
}

