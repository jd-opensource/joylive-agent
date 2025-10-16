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
package com.jd.live.agent.core.util.option;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class OptionTest {

    @Test
    public void testCascade() {
        CascadeOption option = new CascadeOption(null);
        option.put("a.b.c", "123");
        String value = option.getString("a.b.c");
        Assertions.assertEquals("123", value);
    }

    @Test
    public void testCompositeOption() {

        Map<String, Object> map1 = new HashMap<>();
        map1.put("u1", 1);
        Map<String, Object> map2 = new HashMap<>();
        map1.put("u2", 2);
        Option option = CompositeOption.of(MapOption.of(map1), MapOption.of(map2));

        Assertions.assertEquals(1, option.getInteger("u1"));
        Assertions.assertEquals(2, option.getInteger("u2"));
        Assertions.assertNull(option.getString("u3"));

    }

}
