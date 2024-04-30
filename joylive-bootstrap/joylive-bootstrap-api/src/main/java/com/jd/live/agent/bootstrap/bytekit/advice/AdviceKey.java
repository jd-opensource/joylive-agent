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
package com.jd.live.agent.bootstrap.bytekit.advice;

/**
 * AdviceKey class provides static methods for generating unique keys for methods and constructors.
 */
public class AdviceKey {

    /**
     * A constant string representing the constructor.
     */
    private static final String CONSTRUCTOR = "_Constructor_";

    /**
     * Generates a unique key for a given method based on its description, name, and the class loader.
     *
     * @param methodDesc  the description of the method
     * @param methodName  the name of the method
     * @param classLoader the class loader used to load the method's class
     * @return a unique string representing the method key
     */
    public static String getMethodKey(String methodDesc, String methodName, ClassLoader classLoader) {
        return Integer.toHexString(methodDesc.hashCode()) + "_" + methodName + "_" + classLoader;
    }

    /**
     * Generates a unique key for a given constructor based on its description and the class loader.
     *
     * @param methodDesc  the description of the constructor
     * @param classLoader the class loader used to load the constructor's class
     * @return a unique string representing the constructor key
     */
    public static String getConstructorKey(String methodDesc, ClassLoader classLoader) {
        return Integer.toHexString(methodDesc.hashCode()) + CONSTRUCTOR + classLoader;
    }
}
