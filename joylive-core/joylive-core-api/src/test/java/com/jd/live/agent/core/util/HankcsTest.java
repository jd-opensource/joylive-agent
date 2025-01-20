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
package com.jd.live.agent.core.util;

import com.jd.live.agent.core.util.trie.hankcs.AhoCorasickDoubleArrayTrie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class HankcsTest {

    private static AhoCorasickDoubleArrayTrie<Boolean> trie;

    @BeforeAll
    public static void init() {
        trie = new AhoCorasickDoubleArrayTrie<>();
        Map<String, Boolean> map = new HashMap<>();
        map.put("x-live-", Boolean.TRUE);
        map.put("x-service-", Boolean.TRUE);
        map.put("x-lane-", Boolean.TRUE);
        map.put("x-live-space-id", Boolean.TRUE);
        map.put("x-live-rule-id", Boolean.TRUE);
        map.put("x-live-uid", Boolean.TRUE);
        map.put("x-lane-space-id", Boolean.TRUE);
        map.put("x-lane-code", Boolean.TRUE);

        trie.build(map);
    }

    @Test
    void testTrie() {
        Assertions.assertTrue(trie.matches("x-live-space-id"));
        Assertions.assertTrue(trie.matches("x-live-service-id"));
        Assertions.assertTrue(trie.matches("x-lane-space-id"));
        Assertions.assertFalse(trie.matches("x-lane1-space-id"));
    }


}
