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
package com.jd.live.agent.plugin.application.springboot.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Utility for creating virtual thread executors with custom naming.
 * <p>Uses reflection to access Java 21+ virtual thread APIs.
 */
public class ThreadUtils {

    /**
     * Creates an ExecutorService that spawns named virtual threads per task.
     *
     * @param namePrefix prefix for thread names (format: "{prefix}-{seqNum}")
     * @return ExecutorService using virtual threads
     * @throws NoSuchMethodException     if virtual thread APIs are unavailable
     * @throws InvocationTargetException if reflective invocation fails
     * @throws IllegalAccessException    if reflective access is denied
     */
    public static ExecutorService ofVirtualExecutor(String namePrefix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = Thread.class.getDeclaredMethod("ofVirtual");
        Object ofVirtual = method.invoke(null);
        Class<?> ofVirtualClass = ofVirtual.getClass();
        Method nameMethod = ofVirtualClass.getDeclaredMethod("name", String.class, long.class);
        nameMethod.setAccessible(true);
        ofVirtual = nameMethod.invoke(ofVirtual, namePrefix, 1);
        Method factoryMethod = ofVirtualClass.getDeclaredMethod("factory");
        factoryMethod.setAccessible(true);
        Object factory = factoryMethod.invoke(ofVirtual);
        Method executorMethod = Executors.class.getDeclaredMethod("newThreadPerTaskExecutor", ThreadFactory.class);
        return (ExecutorService) executorMethod.invoke(null, (ThreadFactory) factory);
    }
}
