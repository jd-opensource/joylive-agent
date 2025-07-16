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
package com.jd.live.agent.core.util.trie.hankcs;

import com.jd.live.agent.bootstrap.util.Inclusion.InclusionType;
import com.jd.live.agent.bootstrap.util.Inclusion.PredicateFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Creates inclusion predicates using Aho-Corasick string matching algorithm.
 */
public class AhoCorasickPredicateFactory implements PredicateFactory {

    public static final PredicateFactory INSTANCE = new AhoCorasickPredicateFactory();

    @Override
    public BiFunction<String, Function<String, String>, InclusionType> create(Set<String> names, Set<String> prefixes, boolean nullable) {
        // For small rule sets, the performance is lower than the default prefix matching implementation.
        AhoCorasickDoubleArrayTrie<InclusionType> trie = new AhoCorasickDoubleArrayTrie<>();
        trie.build(build(names, prefixes));
        return (s, f) -> {
            if (s == null || s.isEmpty()) {
                return InclusionType.EXCLUDE;
            } else if (trie.isEmpty()) {
                return nullable ? InclusionType.INCLUDE_EMPTY : InclusionType.EXCLUDE;
            } else {
                Hit<InclusionType> hit = trie.findFirst(s);
                return hit == null ? InclusionType.EXCLUDE : hit.value;
            }
        };
    }

    /**
     * Builds the pattern map for the Aho-Corasick trie.
     *
     * @param names    exact match patterns (key=string, value=INCLUDE_EXACTLY)
     * @param prefixes prefix patterns (key=string, value=INCLUDE_OTHER)
     * @return combined pattern map
     */
    private Map<String, InclusionType> build(Set<String> names, Set<String> prefixes) {
        Map<String, InclusionType> map = new HashMap<>();
        if (names != null) {
            names.forEach(name -> {
                map.put(name, InclusionType.INCLUDE_EXACTLY);
            });
        }
        if (prefixes != null) {
            prefixes.forEach(prefix -> {
                map.put(prefix, InclusionType.INCLUDE_OTHER);
            });
        }
        return map;
    }
}
