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
package com.jd.live.agent.core.util.type;

import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValuePathTest {

    @Test
    public void testPath() {
        Map<String, Object> map = new HashMap<>();
        map.put("room1", new Room(Arrays.asList(new Desk(1), new Desk(2))));
        ValuePath valuePath = new ValuePath("room1[2].length");
        Assertions.assertNull(valuePath.get(map));
        valuePath = new ValuePath("room1.desks[2].length");
        Assertions.assertNull(valuePath.get(map));
        valuePath = new ValuePath("room1.desks[1].length");
        Assertions.assertEquals(2, valuePath.get(map));
    }


    @Getter
    private static class Room {

        private final List<Desk> desks;

        Room(List<Desk> desks) {
            this.desks = desks;
        }

    }

    @Getter
    private static class Desk {

        private final int length;

        Desk(int length) {
            this.length = length;
        }

    }


}
