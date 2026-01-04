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
package com.jd.live.agent.bootstrap.bytekit.context;

import lombok.AllArgsConstructor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A utility class for managing a stack of origin methods.
 */
public class OriginStack {

    /**
     * A thread-local stack of origin methods.
     */
    protected static final ThreadLocal<Deque<OriginMethod>> INVOKE_ORIGIN_METHOD_STACK = new ThreadLocal<>();

    /**
     * Pushes a new origin method onto the stack.
     *
     * @param target     the target object of the method
     * @param methodDesc the description of the method
     */
    public static void push(Object target, String methodDesc) {
        Deque<OriginMethod> stack = INVOKE_ORIGIN_METHOD_STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>(8);
            INVOKE_ORIGIN_METHOD_STACK.set(stack);
        }
        stack.push(new OriginMethod(target, methodDesc));
    }

    /**
     * Attempts to pop an origin method from the stack.
     *
     * @param target     the target object of the method
     * @param methodDesc the description of the method
     * @return true if the method was successfully popped, false otherwise
     */
    public static boolean tryPop(Object target, String methodDesc) {
        Deque<OriginMethod> stack = INVOKE_ORIGIN_METHOD_STACK.get();
        OriginMethod result = stack == null ? null : stack.peek();
        // method is always a new instance in bytebuddy, so we use equals to compare
        if (result != null && result.target == target && result.methodDesc.equals(methodDesc)) {
            stack.pop();
            return true;
        }
        return false;
    }

    /**
     * A utility class representing an origin method.
     */
    @AllArgsConstructor
    protected static class OriginMethod {

        private Object target;

        private String methodDesc;

    }
}
