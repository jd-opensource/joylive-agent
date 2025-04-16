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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentMap;

public class MapTest {

    @Test
    void testCaseInsensitiveConcurrentHashMap() {
        ConcurrentMap<String, String> map = new CaseInsensitiveConcurrentMap<>();
        map.put("key1", "value");
        map.put("KEY2", "value");
        map.put("kEy3", "value");
        Assertions.assertEquals("value", map.get("KEY1"));
        Assertions.assertEquals("value", map.get("KEY2"));
        Assertions.assertEquals("value", map.get("KEy3"));

        map.computeIfAbsent("KEY1", k -> "value1");
        map.computeIfPresent("Key2", (k, v) -> "value2");
        map.compute("Key3", (k, v) -> "value3");

        Assertions.assertEquals("value", map.get("KEY1"));
        Assertions.assertEquals("value2", map.get("KEY2"));
        Assertions.assertEquals("value3", map.get("KEy3"));

        map.replace("KeY2", "value4");
        Assertions.assertEquals("value4", map.get("KEY2"));
        map.remove("KEY1");
        Assertions.assertFalse(map.containsKey("KEY1"));
    }
}
