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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.bytekit.context.OriginStack;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;

import static com.jd.live.agent.bootstrap.bytekit.context.MethodContext.ORIGIN_METHOD_CONTEXT;

/**
 * StaticMethodAdvice
 *
 * @since 1.0.0
 */
public class StaticMethodAdvice {

    private StaticMethodAdvice() {
    }

    @SuppressWarnings("all")
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onEnter(@Advice.Origin Method method,
                                  @Advice.Origin("#t\\##m#s") String methodDesc,
                                  @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] arguments,
                                  @Advice.Local(value = "_EXECUTABLE_CONTEXT_$JOYLIVE_LOCAL") Object context
    ) throws Throwable {
        if (OriginStack.tryPop(null, method)) {
            // invoke origin method.
            context = ORIGIN_METHOD_CONTEXT;
            return false;
        }

        MethodContext mc = new MethodContext(null, method, arguments, methodDesc);
        context = mc;
        AdviceHandler.onEnter(mc);
        arguments = mc.getArguments();
        // skipOn = Advice.OnNonDefaultValue.class
        return mc.isSkip();

    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result,
                              @Advice.Thrown(readOnly = false) Throwable throwable,
                              @Advice.Local(value = "_EXECUTABLE_CONTEXT_$JOYLIVE_LOCAL") Object context
    ) throws Throwable {
        MethodContext mc = (MethodContext) context;
        if (mc == ORIGIN_METHOD_CONTEXT) {
            return;
        }
        // invoke enhanced method
        if (!mc.isSkip()) {
            mc.setResult(result);
            mc.setThrowable(throwable);
        }
        AdviceHandler.onExit(mc);
        if (result != mc.getResult()) {
            result = mc.getResult();
        }
        if (throwable != mc.getThrowable()) {
            throwable = mc.getThrowable();
        }
    }
}
