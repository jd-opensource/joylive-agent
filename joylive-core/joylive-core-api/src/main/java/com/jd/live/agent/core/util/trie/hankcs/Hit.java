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

import lombok.Getter;

/**
 * A result output
 *
 * @param <V> the value type
 */
@Getter
public class Hit<V> {
    /**
     * the beginning index, inclusive.
     */
    protected final int begin;
    /**
     * the ending index, exclusive.
     */
    protected final int end;
    /**
     * the value assigned to the keyword
     */
    protected final V value;

    public Hit(int begin, int end, V value) {
        this.begin = begin;
        this.end = end;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("[%d:%d]=%s", begin, end, value);
    }
}
