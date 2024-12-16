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
package com.jd.live.agent.implement.bytekit.bytebuddy.advice;

import com.jd.live.agent.bootstrap.bytekit.advice.AdviceHandler;
import com.jd.live.agent.bootstrap.bytekit.advice.AdviceKey;
import com.jd.live.agent.bootstrap.bytekit.context.ConstructorContext;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Constructor;

/**
 * ConstructorAdvice
 *
 * @since 1.0.0
 */
public class ConstructorAdvice {

    private ConstructorAdvice() {
    }

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Class<?> type,
                               @Advice.Origin Constructor<?> constructor,
                               @Advice.Origin("#t\\##m#s") String methodDesc,
                               @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] arguments,
                               @Advice.Local(value = "_ADVICE_KEY_$JOYLIVE_LOCAL") String adviceKey,
                               @Advice.Local(value = "_EXECUTABLE_CONTEXT_$JOYLIVE_LOCAL") Object context
    ) throws Throwable {
        Class<?> localType = type;
        Constructor<?> localConstructor = constructor;
        String localMehotdDesc = methodDesc;
        adviceKey = AdviceKey.getMethodKey(localMehotdDesc, localType.getClassLoader());
        ConstructorContext cc = new ConstructorContext(localType, arguments, localConstructor, localMehotdDesc);
        context = cc;
        AdviceHandler.onEnter(cc, adviceKey);
        arguments = cc.getArguments();
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.This(typing = Assigner.Typing.DYNAMIC) Object result,
                              @Advice.Local(value = "_ADVICE_KEY_$JOYLIVE_LOCAL") String adviceKey,
                              @Advice.Local(value = "_EXECUTABLE_CONTEXT_$JOYLIVE_LOCAL") Object context
    ) throws Throwable {
        ConstructorContext cc = (ConstructorContext) context;
        cc.setTarget(result);
        AdviceHandler.onExit(cc, adviceKey);
    }
}
