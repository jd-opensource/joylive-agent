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

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Immutable key representing a method's signature for caching purposes.
 * Identifies methods by declaring class, name, parameter types and return type.
 * Optimized for fast hashCode() and equals() comparisons.
 */
public final class MethodSignature {
    private final String declaringClassName;
    private final String methodName;
    private final String[] parameterTypeNames;
    private final int hashCode;

    public MethodSignature(Method method) {
        this.declaringClassName = method.getDeclaringClass().getName();
        this.methodName = method.getName();
        this.parameterTypeNames = getParameterTypeNames(method);
        this.hashCode = computeHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodSignature)) return false;
        MethodSignature that = (MethodSignature) o;
        return declaringClassName.equals(that.declaringClassName)
                && methodName.equals(that.methodName)
                && Arrays.equals(parameterTypeNames, that.parameterTypeNames);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Extracts parameter type names from a method.
     *
     * @param method the method to inspect
     * @return array of fully-qualified parameter type names
     */
    private String[] getParameterTypeNames(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        String[] names = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            names[i] = paramTypes[i].getName();
        }
        return names;
    }

    private int computeHashCode() {
        return 31 * (31 * declaringClassName.hashCode() + methodName.hashCode())
                + Arrays.hashCode(parameterTypeNames);
    }
}
