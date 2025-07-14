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

/**
 * Functional interface for invoking methods on target objects.
 * Similar to {@link java.lang.reflect.Method#invoke} but with simpler exception handling.
 */
public interface MethodInvoker {

    /**
     * Invokes the method on the target object with given arguments.
     *
     * @param target the object to invoke the method on
     * @param args   method arguments (varargs)
     * @return method invocation result
     * @throws Throwable any exception thrown by the invoked method
     */
    Object invoke(Object target, Object... args) throws Throwable;

}
