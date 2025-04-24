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
package com.jd.live.agent.bootstrap.util.type;

import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

public class UnsafeFieldAccessorFactoryTest {

    @Test
    void testGetQuietly() {
        Apple apple = new Apple("apple", "red");
        Assertions.assertEquals("apple", getQuietly(apple, "name"));
    }

    @Getter
    private static class Fruit {
        protected String name;

        Fruit(String name) {
            this.name = name;
        }
    }

    @Getter
    private static class Apple extends Fruit {

        private final String color;

        Apple(String name, String color) {
            super(name);
            this.color = color;
        }
    }
}
