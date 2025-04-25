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

import java.lang.reflect.Field;
import java.util.function.Function;

public class UnsafeFieldAccessorFactoryTest {

    @Test
    void testAccessor() {
        //  --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try {
            testAccessor(UnsafeFieldAccessorFactory.getJDKUnsafe(classLoader));
        } catch (Throwable ignore) {
        }
        try {
            testAccessor(UnsafeFieldAccessorFactory.getSunUnsafe(classLoader));
        } catch (Throwable ignore) {
        }
        testAccessor(UnsafeFieldAccessorFactory.ReflectFieldAccessor::new);
    }

    protected void testAccessor(Function<Field, UnsafeFieldAccessor> function) {
        Apple apple = new Apple("apple", "red", 1);
        UnsafeFieldAccessorFactory.setValue(apple, "name", "orange");
        UnsafeFieldAccessorFactory.setValue(apple, "color", "blue");
        UnsafeFieldAccessorFactory.setValue(apple, "weight", 2);
        Assertions.assertEquals("orange", UnsafeFieldAccessorFactory.getQuietly(apple, "name", function, null));
        Assertions.assertEquals("blue", UnsafeFieldAccessorFactory.getQuietly(apple, "color", function, null));
        Assertions.assertEquals(2, (int) UnsafeFieldAccessorFactory.getQuietly(apple, "weight", function, null));

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

        private final int weight;

        Apple(String name, String color, int weight) {
            super(name);
            this.color = color;
            this.weight = weight;
        }
    }
}
