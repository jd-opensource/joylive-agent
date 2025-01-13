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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.jd.live.agent.core.util.CollectionUtils.lookup;

public class CollectionUtilsTest {

    @Test
    void testLookup() {
        Integer[] sources = new Integer[]{1, 2, 3, 4, 1, 1, 7, 8, 9, 10};

        LookupIndex index = lookup(sources, sources.length, 1, v -> v.equals(1));
        Assertions.assertEquals(3, index.size());
        index = lookup(sources, sources.length, 2, v -> v.equals(1));
        Assertions.assertEquals(2, index.size());
        index = lookup(sources, 3, 1, v -> v.equals(1));
        Assertions.assertEquals(1, index.size());
        index = lookup(sources, 3, 1, v -> v.equals(3));
        Assertions.assertEquals(1, index.size());
        index = lookup(sources, 3, 1, v -> v.equals(4));
        Assertions.assertNull(index);
        index = lookup(sources, sources.length, 1, v -> v.equals(2));
        Assertions.assertEquals(1, index.size());
        lookup(sources, sources.length, 1, v -> v.equals(55));
        Assertions.assertEquals(1, index.size());
    }


}
