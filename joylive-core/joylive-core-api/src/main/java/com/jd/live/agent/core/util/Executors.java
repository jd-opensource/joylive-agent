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

import java.util.concurrent.Callable;

/**
 * Utility class for executing tasks with a specific context class loader.
 */
public class Executors {

    /**
     * Executes the specified Runnable using the provided ClassLoader as the context class loader.
     * If the ClassLoader is null, the Runnable is executed with the current context class loader.
     * After execution, the original context class loader is restored.
     *
     * @param classLoader the ClassLoader to be set as the context class loader for the execution of the Runnable (can be null)
     * @param runnable    the Runnable to be executed (can be null)
     */
    public static void execute(ClassLoader classLoader, Runnable runnable) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            runnable.run();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    /**
     * Executes the specified Callable using the provided ClassLoader as the context class loader.
     * After execution, the original context class loader is restored.
     *
     * @param classLoader the ClassLoader to be set as the context class loader for the execution of the Callable
     * @param callable    the Callable to be executed
     * @param <T>         the type of the result returned by the Callable
     * @return the result of the Callable's computation
     * @throws Exception if the Callable throws an exception
     */
    public static <T> T execute(ClassLoader classLoader, Callable<T> callable) throws Exception {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            return callable.call();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }
}


