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

import lombok.Getter;

/**
 * Represents a path with a specific match type.
 */
public interface Path {

    /**
     * Gets the path.
     *
     * @return the path as a String.
     */
    String getPath();

    /**
     * Gets the match type of the path.
     *
     * @return the match type as a {@link PathMatchType}.
     */
    PathMatchType getMatchType();

    /**
     * A specific implementation of {@link Path} that represents a path
     * with a prefix match type.
     */
    @Getter
    class PrefixPath implements Path {

        private final String path;

        private final PathMatchType matchType = PathMatchType.PREFIX;

        /**
         * Constructs a new {@code PrefixPath} with the specified path.
         *
         * @param path the path as a String.
         */
        public PrefixPath(String path) {
            this.path = path;
        }

    }

}