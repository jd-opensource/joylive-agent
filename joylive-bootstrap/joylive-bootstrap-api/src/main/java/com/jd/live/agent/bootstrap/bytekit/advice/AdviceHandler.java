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

import com.jd.live.agent.bootstrap.bytekit.advice.AdviceExecution.EnterExecution;
import com.jd.live.agent.bootstrap.bytekit.advice.AdviceExecution.ErrorExecution;
import com.jd.live.agent.bootstrap.bytekit.advice.AdviceExecution.ExiteExecution;
import com.jd.live.agent.bootstrap.bytekit.advice.AdviceExecution.SuccessExecution;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A handler class for managing advices and their associated interceptors. Provides static methods
 * for handling entry and exit points of method execution contexts, as well as managing advice lifecycle.
 */
public class AdviceHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdviceHandler.class);

    /**
     * A concurrent map that holds all advices identified by their unique keys.
     */
    private static final Map<Object, AdviceDesc> advices = new ConcurrentHashMap<>(1000);

    public static Consumer<Throwable> onException;

    /**
     * Private constructor to prevent instantiation.
     */
    private AdviceHandler() {
    }

    /**
     * Handles the entry point for a given execution context and advice key.
     *
     * @param context   the execution context
     * @param adviceKey the unique key of the advice
     * @throws Throwable if any exception occurs during interception
     */
    public static void onEnter(final ExecutableContext context, final Object adviceKey) throws Throwable {
        if (context == null || adviceKey == null) {
            return;
        }
        AdviceDesc adviceDesc = advices.get(adviceKey);
        List<Interceptor> interceptors = adviceDesc == null ? null : adviceDesc.getInterceptors();
        int size = interceptors == null ? 0 : interceptors.size();
        if (size == 1) {
            onEnter(context, interceptors.get(0));
        } else if (size > 1) {
            for (Interceptor interceptor : interceptors) {
                if (onEnter(context, interceptor)) {
                    break;
                }
            }
        }
    }

    /**
     * Handles the exit point for a given execution context and advice key.
     *
     * @param context   the execution context
     * @param adviceKey the unique key of the advice
     * @throws Throwable if any exception occurs during interception
     */
    public static void onExit(final ExecutableContext context, final Object adviceKey) throws Throwable {
        AdviceDesc adviceDesc = advices.get(adviceKey);
        List<Interceptor> interceptors = adviceDesc == null ? null : adviceDesc.getInterceptors();
        int size = interceptors == null ? 0 : interceptors.size();
        if (size == 1) {
            onExit(context, interceptors.get(0));
        } else if (size > 1) {
            // reverse order
            for (int i = size - 1; i >= 0; i--) {
                onExit(context, interceptors.get(i));
            }
        }
    }

    /**
     * Handles the entry point of an executable context by invoking the onEnter method of the given interceptor.
     *
     * @param context     the executable context
     * @param interceptor the interceptor to be invoked
     * @return true if the execution should be skipped, false otherwise
     * @throws Throwable if an error occurs during the execution of the interceptor's onEnter method
     */
    private static boolean onEnter(final ExecutableContext context, final Interceptor interceptor) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("enter [%s], interceptor is [%s].", context.getDescription(), interceptor.getClass().getName()));
        }
        handle(context, interceptor, EnterExecution.INSTANCE, "enter");
        return context.isSkip();
    }

    /**
     * Handles the exit point of an executable context by invoking the appropriate method of the given interceptor.
     *
     * @param context     the executable context
     * @param interceptor the interceptor to be invoked
     * @throws Throwable if an error occurs during the execution of the interceptor's methods
     */
    private static void onExit(final ExecutableContext context, final Interceptor interceptor) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("exit [%s], interceptor is [%s].", context.getDescription(), interceptor.getClass().getName()));
        }
        if (context.isSuccess()) {
            handle(context, interceptor, SuccessExecution.INSTANCE, "success");
        } else {
            if (onException != null) {
                onException.accept(context.getThrowable());
            }
            handle(context, interceptor, ErrorExecution.INSTANCE, "recover");
        }
        handle(context, interceptor, ExiteExecution.INSTANCE, "exit");
    }

    /**
     * Generic method for handling interception actions.
     *
     * @param context     the execution context
     * @param interceptor the interceptor to be executed
     * @param execution   the action to be performed by the interceptor
     * @param action      the name of the action being performed
     * @throws Throwable if any exception occurs during interception
     */
    private static void handle(final ExecutableContext context,
                               final Interceptor interceptor,
                               final AdviceExecution execution,
                               final String action) throws Throwable {
        try {
            execution.execute(interceptor, context);
        } catch (Throwable t) {
            logger.error(String.format("failed to %s %s, caused by %s", action, context.getDescription(), t.getMessage()), t);
            execution.fail(t, context);
        }
    }

    /**
     * Retrieves or creates an AdviceDesc instance for the given advice key.
     *
     * @param adviceKey the unique key of the advice
     * @return the AdviceDesc instance
     */
    public static AdviceDesc getOrCreate(final Object adviceKey) {
        return advices.computeIfAbsent(adviceKey, AdviceDesc::new);
    }

    /**
     * Removes advice identified by its unique key.
     *
     * @param adviceKey the unique key of the advice to be removed
     */
    public static void remove(final Object adviceKey) {
        advices.remove(adviceKey);
    }

}

