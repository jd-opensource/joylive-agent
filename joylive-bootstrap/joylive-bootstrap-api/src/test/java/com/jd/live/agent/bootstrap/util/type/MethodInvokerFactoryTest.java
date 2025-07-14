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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Comprehensive tests for {@link MethodInvokerFactory} covering:
 * <ul>
 *   <li>Core functionality for all method types</li>
 *   <li>Reflection-specific edge cases</li>
 *   <li>Version compatibility</li>
 *   <li>Performance benchmarks</li>
 * </ul>
 */
public class MethodInvokerFactoryTest {

    // === Shared Test Resources ===
    private static final Method ADD_METHOD;
    private static final Method PREFIX_METHOD;
    private static final Method SQUARE_METHOD;

    static {
        try {
            ADD_METHOD = TestClass.class.getMethod("add", int.class, int.class);
            PREFIX_METHOD = TestClass.class.getDeclaredMethod("prefix", String.class);
            SQUARE_METHOD = TestClass.class.getMethod("square", double.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Test setup failed", e);
        }
    }

    // === Core Functionality Tests ===
    @Nested
    @DisplayName("Core Functionality Tests")
    class CoreTests {
        @Test
        @DisplayName("Should invoke public instance method")
        void publicInstanceMethod() throws Throwable {
            MethodInvoker invoker = MethodInvokerFactory.getInvoker(ADD_METHOD);
            assertEquals(5, invoker.invoke(new TestClass(), 2, 3));
        }

        @Test
        @DisplayName("Should invoke private method when made accessible")
        void privateMethod() throws Throwable {
            MethodInvoker invoker = MethodInvokerFactory.getInvoker(PREFIX_METHOD);
            assertEquals("test_value", invoker.invoke(new TestClass(), "value"));
        }

        @Test
        @DisplayName("Should invoke static method")
        void staticMethod() throws Throwable {
            MethodInvoker invoker = MethodInvokerFactory.getInvoker(SQUARE_METHOD);
            assertEquals(9.0, invoker.invoke(null, 3.0));
        }

        @Test
        @DisplayName("Should handle method signature mismatches")
        void testSignatureMismatch() throws Throwable {
            MethodInvoker invoker = MethodInvokerFactory.getInvoker(ADD_METHOD);
            assertThrows(ClassCastException.class,
                    () -> invoker.invoke(new TestClass(), "wrong", "args"));
        }
    }

    // === Reflection-specific Tests ===
    @Nested
    @DisplayName("Reflection-specific Tests")
    class ReflectionTests {

        @Test
        @DisplayName("Should invoke public instance method")
        void publicInstanceMethod() throws Throwable {
            MethodInvoker invoker = MethodInvokerFactory.getInvoker(ADD_METHOD, MethodVersion.REFLECT);
            assertEquals(5, invoker.invoke(new TestClass(), 2, 3));
        }

        @Test
        @DisplayName("Should invoke private method when made accessible")
        void privateMethod() throws Throwable {
            MethodInvoker invoker = MethodInvokerFactory.getInvoker(PREFIX_METHOD, MethodVersion.REFLECT);
            assertEquals("test_value", invoker.invoke(new TestClass(), "value"));
        }

        @Test
        @DisplayName("Should invoke static method")
        void staticMethod() throws Throwable {
            MethodInvoker invoker = MethodInvokerFactory.getInvoker(SQUARE_METHOD, MethodVersion.REFLECT);
            assertEquals(9.0, invoker.invoke(null, 3.0));
        }
    }

    // === Performance Tests ===
    @Test
    @DisplayName("Performance Comparison")
    void performanceComparison() throws Throwable {
        // Setup
        TestClass instance = new TestClass();
        MethodInvoker reflectInvoker = MethodInvokerFactory.getInvoker(ADD_METHOD, MethodVersion.REFLECT);
        MethodInvoker handleInvoker = MethodInvokerFactory.getInvoker(ADD_METHOD);

        // Reflection Test
        PerformanceTester.run("Reflection Invocation",
                () -> {
                    try {
                        reflectInvoker.invoke(instance, 2, 3);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                },
                10,    // Warmup iterations
                100000  // Test iterations
        );

        // MethodHandle Test
        PerformanceTester.run("MethodHandle Invocation",
                () -> {
                    try {
                        handleInvoker.invoke(instance, 2, 3);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                },
                10,    // Warmup iterations
                100000  // Test iterations
        );
    }

    // === Test Class ===
    private static class TestClass {
        public int add(int a, int b) {
            return a + b;
        }

        private String prefix(String s) {
            return "test_" + s;
        }

        private void privateMethod() {
        }

        public static double square(double x) {
            return x * x;
        }
    }

    // === Performance Test Utility ===
    private static class PerformanceTester {
        static void run(String testName, Runnable test, int warmup, int iterations) {
            // Warmup
            for (int i = 0; i < warmup; i++) {
                test.run();
            }

            // Test
            long totalTime = 0;
            long minTime = Long.MAX_VALUE;
            long maxTime = Long.MIN_VALUE;

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                test.run();
                long duration = System.nanoTime() - start;

                totalTime += duration;
                minTime = Math.min(minTime, duration);
                maxTime = Math.max(maxTime, duration);
            }

            // Results
            double avgTime = (double) totalTime / iterations;
            System.out.println("\n=== " + testName + " ===");
            System.out.printf("Iterations: %d\n", iterations);
            System.out.printf("Avg Time: %8.2f ns\n", avgTime);
            System.out.printf("Min Time: %8d ns\n", minTime);
            System.out.printf("Max Time: %8d ns\n", maxTime);
            System.out.printf("Total Time: %d ms\n",
                    TimeUnit.NANOSECONDS.toMillis(totalTime));
        }
    }
}