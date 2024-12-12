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
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * A utility class for managing a stack of origin methods.
 */
public class OriginStack {

    /**
     * A thread-local stack of origin methods.
     */
    protected static ThreadLocal<LinkedList<OriginMethod>> INVOKE_ORIGIN_METHOD_STACK;

    /**
     * Pushes a new origin method onto the stack.
     *
     * @param target the target object of the method
     * @param method the method itself
     */
    public static void push(Object target, Method method) {
        if (INVOKE_ORIGIN_METHOD_STACK == null) {
            INVOKE_ORIGIN_METHOD_STACK = ThreadLocal.withInitial(LinkedList::new);
        }
        LinkedList<OriginMethod> stack = INVOKE_ORIGIN_METHOD_STACK.get();
        stack.push(new OriginMethod(target, method));
    }

    /**
     * Attempts to pop an origin method from the stack.
     *
     * @param target the target object of the method
     * @param method the method itself
     * @return true if the method was successfully popped, false otherwise
     */
    public static boolean tryPop(Object target, Method method) {
        if (INVOKE_ORIGIN_METHOD_STACK == null) {
            return false;
        }
        LinkedList<OriginMethod> stack = INVOKE_ORIGIN_METHOD_STACK.get();
        OriginMethod result = stack.peek();
        if (result != null && result.getTarget() == target && result.getMethod().equals(method)) {
            stack.pop();
            return true;
        }
        return false;
    }

    /**
     * A utility class representing an origin method.
     */
    @Getter
    @AllArgsConstructor
    protected static class OriginMethod {

        private Object target;

        private Method method;

    }
}
