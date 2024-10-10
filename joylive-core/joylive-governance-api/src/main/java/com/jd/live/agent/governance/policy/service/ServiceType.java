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
package com.jd.live.agent.governance.policy.service;

import com.jd.live.agent.core.util.trie.PathMatchType;
import com.jd.live.agent.core.util.trie.PathType;

/**
 * Enum representing different types of services and their corresponding path and match types.
 */
public enum ServiceType {

    /**
     * Represents an HTTP service type.
     */
    HTTP {
        @Override
        public PathType getPathType() {
            return PathType.URL;
        }

        @Override
        public PathMatchType getMatchType() {
            return PathMatchType.PREFIX;
        }

        @Override
        public String normalize(String path) {
            return path == null || path.isEmpty() ? "/" : path;
        }
    },

    /**
     * Represents an RPC service type that is identified by the application.
     */
    RPC_APP {
        @Override
        public PathType getPathType() {
            return PathType.INTERFACE;
        }

        @Override
        public PathMatchType getMatchType() {
            return PathMatchType.EQUAL;
        }

        @Override
        public String normalize(String path) {
            // TODO why ""?
            return path == null || path.isEmpty() ? "" : path;
        }
    },

    /**
     * Represents an RPC service type that is identified by the interface.
     */
    RPC_INTERFACE {
        @Override
        public PathType getPathType() {
            return PathType.INTERFACE;
        }

        @Override
        public PathMatchType getMatchType() {
            return PathMatchType.EQUAL;
        }

        @Override
        public String normalize(String path) {
            return path == null || path.isEmpty() ? "/" : path;
        }
    };

    /**
     * Gets the path type associated with the service type.
     *
     * @return The {@link PathType} associated with the service type.
     */
    public abstract PathType getPathType();

    /**
     * Gets the match type associated with the service type.
     *
     * @return The {@link PathMatchType} associated with the service type.
     */
    public abstract PathMatchType getMatchType();

    /**
     * Normalizes the given path based on the service type.
     *
     * @param path The path to be normalized. It can be {@code null} or empty.
     * @return The normalized path.
     */
    public abstract String normalize(String path);

}
