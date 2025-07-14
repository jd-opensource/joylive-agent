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

import java.lang.invoke.MethodHandle;

/**
 * Efficient invoker for MethodHandle with automatic unboxing support.
 * Handles both static and instance methods with optimized dispatch paths.
 */
@Getter
public class MethodHandleCaller implements MethodInvoker {

    private final MethodHandle handle;

    private final boolean staticMethod;

    public MethodHandleCaller(MethodHandle handle, boolean staticMethod) {
        this.handle = handle;
        this.staticMethod = staticMethod;
    }

    @Override
    public Object invoke(Object target, Object... args) throws Throwable {
        if (staticMethod) {
            return invokeStaticMethod(args);
        } else {
            return invokeInstanceMethod(target, args);
        }
    }

    /**
     * Invokes an instance method with exact type matching.
     * Contains optimized paths for 0-7 arguments.
     */
    private Object invokeInstanceMethod(Object target, Object... args) throws Throwable {
        switch (args.length) {
            case 0:
                return handle.invoke(target);
            case 1:
                return handle.invoke(target, args[0]);
            case 2:
                return handle.invoke(target, args[0], args[1]);
            case 3:
                return handle.invoke(target, args[0], args[1], args[2]);
            case 4:
                return handle.invoke(target, args[0], args[1], args[2], args[3]);
            case 5:
                return handle.invoke(target, args[0], args[1], args[2], args[3], args[4]);
            case 6:
                return handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5]);
            case 7:
                return handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            case 8:
                return handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
            case 9:
                return handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
            case 10:
                return handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
            default:
                Object[] newArgs = new Object[args.length + 1];
                newArgs[0] = target;
                System.arraycopy(args, 0, newArgs, 1, args.length);
                return handle.invoke(newArgs);
        }
    }

    /**
     * Invokes a static method with automatic unboxing conversions.
     * Contains optimized paths for 0-7 arguments.
     */
    private Object invokeStaticMethod(Object... args) throws Throwable {
        switch (args.length) {
            case 0:
                return handle.invoke();
            case 1:
                return handle.invoke(args[0]);
            case 2:
                return handle.invoke(args[0], args[1]);
            case 3:
                return handle.invoke(args[0], args[1], args[2]);
            case 4:
                return handle.invoke(args[0], args[1], args[2], args[3]);
            case 5:
                return handle.invoke(args[0], args[1], args[2], args[3], args[4]);
            case 6:
                return handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5]);
            case 7:
                return handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            case 8:
                return handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
            case 9:
                return handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
            case 10:
                return handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
            default:
                return handle.asSpreader(Object[].class, args.length).invoke(args);
        }
    }
}
