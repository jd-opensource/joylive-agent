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
package com.jd.live.agent.governance.policy.lane;

import com.jd.live.agent.core.util.trie.PathMatchType;
import com.jd.live.agent.core.util.trie.PathMatcherTrie;
import com.jd.live.agent.core.util.trie.PathTrie;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class LaneDomain {

    @Getter
    @Setter
    private String host;

    @Getter
    @Setter
    private List<LanePath> paths;

    private final transient PathTrie<LanePath> pathTrie = new PathMatcherTrie<>(() -> paths);

    public LanePath getPath(String path) {
        return pathTrie.match(path, PathMatchType.PREFIX);
    }

    public int getPathSize() {
        return paths == null ? 0 : paths.size();
    }

    public void cache() {
        getPath("");
    }

}
