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

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;

/**
 * Functional interface for handling intercepted method execution.
 */
public interface AdviceExecution {
    /**
     * Processes the intercepted method call.
     *
     * @param interceptor the interceptor instance
     * @param ctx         the execution context
     */
    void execute(Interceptor interceptor, ExecutableContext ctx);

    /**
     * Handles an exception that occurred during execution.
     *
     * @param ctx the execution context
     * @param t   the thrown exception
     * @throws Throwable may rethrow or throw a different exception
     */
    void fail(Throwable t, ExecutableContext ctx) throws Throwable;

    /**
     * Handler for method entry interception.
     */
    class EnterExecution implements AdviceExecution {

        public static final AdviceExecution INSTANCE = new EnterExecution();

        @Override
        public void execute(Interceptor interceptor, ExecutableContext ctx) {
            interceptor.onEnter(ctx);
        }

        @Override
        public void fail(Throwable t, ExecutableContext ctx) throws Throwable {
            if (ctx instanceof MethodContext) {
                ((MethodContext) ctx).skipWithThrowable(t);
            } else {
                throw t;
            }
        }
    }

    /**
     * Handler for successful method execution interception.
     */
    class SuccessExecution implements AdviceExecution {

        public static final AdviceExecution INSTANCE = new SuccessExecution();

        @Override
        public void execute(Interceptor interceptor, ExecutableContext ctx) {
            interceptor.onSuccess(ctx);
        }

        @Override
        public void fail(Throwable t, ExecutableContext ctx) throws Throwable {
            ctx.setThrowable(t);
        }
    }

    /**
     * Handler for error case interception (when method throws exception).
     */
    class ErrorExecution implements AdviceExecution {

        public static final AdviceExecution INSTANCE = new ErrorExecution();

        @Override
        public void execute(Interceptor interceptor, ExecutableContext ctx) {
            interceptor.onError(ctx);
        }

        @Override
        public void fail(Throwable t, ExecutableContext ctx) throws Throwable {
            ctx.setThrowable(t);
        }
    }

    /**
     * Handler for method exit interception (always called after success/error).
     */
    class ExiteExecution implements AdviceExecution {

        public static final AdviceExecution INSTANCE = new ExiteExecution();

        @Override
        public void execute(Interceptor interceptor, ExecutableContext ctx) {
            interceptor.onExit(ctx);
        }

        @Override
        public void fail(Throwable t, ExecutableContext ctx) throws Throwable {
            ctx.setThrowable(t);
        }
    }
}
