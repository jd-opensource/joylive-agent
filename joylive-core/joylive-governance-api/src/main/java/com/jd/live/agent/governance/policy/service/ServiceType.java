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

public enum ServiceType {

    HTTP {
        @Override
        public PathType getPathType() {
            return PathType.URL;
        }

        @Override
        public PathMatchType getMatchType() {
            return PathMatchType.PREFIX;
        }
    },

    RPC_APP {
        @Override
        public PathType getPathType() {
            return PathType.INTERFACE;
        }

        @Override
        public PathMatchType getMatchType() {
            return PathMatchType.EQUAL;
        }
    },

    RPC_INTERFACE {
        @Override
        public PathType getPathType() {
            return PathType.INTERFACE;
        }

        @Override
        public PathMatchType getMatchType() {
            return PathMatchType.EQUAL;
        }
    };

    public abstract PathType getPathType();

    public abstract PathMatchType getMatchType();

}
