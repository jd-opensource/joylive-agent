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

import com.jd.live.agent.core.util.map.MapBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class PathMapTrie<T extends Path> extends MapTrie<T> implements PathTrie<T> {

    private int[] lengths;

    public PathMapTrie(MapBuilder<String, T> cacheBuilder) {
        super(cacheBuilder);
    }

    @Override
    protected Map<String, T> build() {
        Map<String, T> result = super.build();
        int size = result.size();
        switch (size) {
            case 0:
                lengths = new int[0];
                break;
            case 1:
                lengths = new int[1];
                String next = result.keySet().iterator().next();
                lengths[0] = next == null ? 0 : next.length();
                break;
            default:
                Set<Integer> lens = new HashSet<>(size);
                result.keySet().forEach(v -> lens.add(v == null ? 0 : v.length()));
                lengths = new int[lens.size()];
                int index = 0;
                for (Integer len : lens) {
                    lengths[index++] = len;
                }
                Arrays.sort(lengths);
        }
        return result;
    }

    @Override
    public T match(String key, String suffix, char delimiter, boolean withDelimiter) {
        Map<String, T> cache = getCache();
        if (key == null || cache.isEmpty()) {
            return null;
        }
        Function<String, String> appender = suffix == null || suffix.isEmpty() ? v -> v : v -> v + suffix;
        key = convert(key);
        String searchKey = appender.apply(key);
        int index = lengths.length - 1;
        int maxLength = lengths[index];
        int minLength = lengths[0];
        int length = searchKey.length();
        T result = null;
        int pos;
        int slice = 0;
        while (length >= minLength) {
            if (length <= maxLength) {
                result = cache.get(searchKey);
                if (result != null && (result.getMatchType() == PathMatchType.PREFIX || slice == 0)
                        || index == 0) {
                    break;
                }
                maxLength = lengths[--index];
            }
            pos = key.lastIndexOf(delimiter, maxLength);
            if (pos >= 0) {
                key = key.substring(0, pos >= maxLength || !withDelimiter ? pos : pos + 1);
                searchKey = appender.apply(key);
                length = searchKey.length();
                slice++;
            } else {
                result = null;
                break;
            }
        }
        return result;
    }
}
