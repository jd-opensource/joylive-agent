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

public class OptionTest {

    @Test
    public void testCascade() {
        CascadeOption option = new CascadeOption(null);
        option.put("a.b.c", "123");
        String value = option.getString("a.b.c");
        Assertions.assertEquals("123", value);
    }

}
