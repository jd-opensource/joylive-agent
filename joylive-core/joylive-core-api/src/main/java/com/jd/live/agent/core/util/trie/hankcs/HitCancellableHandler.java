/*
 * AhoCorasickDoubleArrayTrie Project
 *      https://github.com/hankcs/AhoCorasickDoubleArrayTrie
 *
 * Copyright 2008-2016 hankcs <me@hankcs.com>
 * You may modify and redistribute as long as this attribution remains.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.util.trie.hankcs;

/**
 * Callback that allows to cancel the search process.
 *
 * @param <V> the value type
 */
public interface HitCancellableHandler<V> {
    /**
     * Hit a keyword, you can use some code like text.substring(begin, end) to get the keyword
     *
     * @param begin the beginning index, inclusive.
     * @param end   the ending index, exclusive.
     * @param value the value assigned to the keyword
     * @return Return true for continuing the search and false for stopping it.
     */
    boolean hit(int begin, int end, V value);
}
