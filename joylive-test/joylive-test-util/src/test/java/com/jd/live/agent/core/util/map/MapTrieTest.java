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
package com.jd.live.agent.core.util.map;

import com.jd.live.agent.core.util.trie.Path;
import com.jd.live.agent.core.util.trie.PathMapTrie;
import com.jd.live.agent.core.util.trie.PathMatchType;
import com.jd.live.agent.core.util.trie.PathTrie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class MapTrieTest {

    @Test
    public void testFastPath() {
        List<PathPolicy> policies = new ArrayList<>();
        policies.add(new PathPolicy("/abc/efg/"));
        policies.add(new PathPolicy("/abc/efg/xxx/ggg/kkk/yy"));
        PathTrie<PathPolicy> trie = new PathMapTrie<>(new ListBuilder<>(() -> policies, PathPolicy::getPath));
        PathPolicy policy = trie.match("/abc/efg/yyyy/kkk/ggg/ddd/zzz", '/', true);
        Assertions.assertNotNull(policy);
        Assertions.assertEquals(policy.getPath(), "/abc/efg/");
        policy = trie.match("/abc/ddd/xxx/dddd/", '/', true);
        Assertions.assertNull(policy);
    }

    @Test
    public void testEqual() {
        List<PathPolicy> policies = new ArrayList<>();
        policies.add(new PathPolicy("/"));
        policies.add(new PathPolicy("/abc/efg/", PathMatchType.EQUAL));
        PathTrie<PathPolicy> trie = new PathMapTrie<>(new ListBuilder<>(() -> policies, PathPolicy::getPath));
        PathPolicy policy = trie.match("/abc/efg/yyyy/kkk/ggg/ddd/zzz", '/', true);
        Assertions.assertNotNull(policy);
        Assertions.assertEquals(policy.getPath(), "/");
    }

    @Test
    public void testPackage() {
        List<PathPolicy> policies = new ArrayList<>();
        policies.add(new PathPolicy("java"));
        policies.add(new PathPolicy("java.lang"));
        PathTrie<PathPolicy> trie = new PathMapTrie<>(new ListBuilder<>(() -> policies, PathPolicy::getPath));
        PathPolicy policy = trie.match("java.lang.util", '.', false);
        Assertions.assertNotNull(policy);
        Assertions.assertEquals(policy.getPath(), "java.lang");
    }

    private class PathPolicy implements Path {
        String path;

        PathMatchType matchType;

        private PathPolicy(String path) {
            this(path, PathMatchType.PREFIX);
        }

        private PathPolicy(String path, PathMatchType matchType) {
            this.path = path;
            this.matchType = matchType;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public PathMatchType getMatchType() {
            return matchType;
        }
    }
}
