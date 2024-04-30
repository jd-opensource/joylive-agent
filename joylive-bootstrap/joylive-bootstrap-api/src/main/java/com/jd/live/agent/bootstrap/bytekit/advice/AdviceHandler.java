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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static com.jd.live.agent.bootstrap.bytekit.advice.ExceptionHandler.EMPTY_EXCEPTION_HANDLER;

/**
 * A handler class for managing advices and their associated interceptors. Provides static methods
 * for handling entry and exit points of method execution contexts, as well as managing advice lifecycle.
 */
public class AdviceHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdviceHandler.class);

    /**
     * A concurrent map that holds all advices identified by their unique keys.
     */
    private static final Map<String, AdviceDesc> advices = new ConcurrentHashMap<>(1000);

    /**
     * Private constructor to prevent instantiation.
     */
    private AdviceHandler() {
    }

    /**
     * Handles the entry point for a given execution context and advice key.
     *
     * @param <T>       the type of the execution context
     * @param context   the execution context
     * @param adviceKey the unique key of the advice
     * @throws Throwable if any exception occurs during interception
     */
    public static <T extends ExecutableContext> void onEnter(T context, String adviceKey) throws Throwable {
        if (context == null || adviceKey == null)
            return;
        AdviceDesc adviceDesc = advices.get(adviceKey);
        List<Interceptor> interceptors = adviceDesc == null ? null : adviceDesc.getInterceptors();
        if (interceptors != null) {
            onEnter(context, interceptors, EMPTY_EXCEPTION_HANDLER);
        }
    }

    /**
     * Handles the entry point for a given execution context and a list of interceptors.
     *
     * @param <T>              the type of the execution context
     * @param context          the execution context
     * @param interceptors     the list of interceptors to be executed
     * @param exceptionHandler the handler for exceptions
     * @throws Throwable if any exception occurs during interception
     */
    public static <T extends ExecutableContext> void onEnter(T context,
                                                             List<Interceptor> interceptors,
                                                             ExceptionHandler exceptionHandler) throws Throwable {
        if (context == null || interceptors == null)
            return;
        for (Interceptor interceptor : interceptors) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("enter [%s], interceptor is [%s].", context.getDescription(), interceptor.getClass().getName()));
            }
            handle(context, interceptor, Interceptor::onEnter, exceptionHandler, "enter");
            if (!context.isSuccess()) {
                throw context.getThrowable();
            } else if (context.isSkip()) {
                break;
            }
        }

    }

    /**
     * Handles the exit point for a given execution context and advice key.
     *
     * @param <T>       the type of the execution context
     * @param context   the execution context
     * @param adviceKey the unique key of the advice
     * @throws Throwable if any exception occurs during interception
     */
    public static <T extends ExecutableContext> void onExit(T context, String adviceKey) throws Throwable {
        AdviceDesc adviceDesc = advices.get(adviceKey);
        List<Interceptor> interceptors = adviceDesc == null ? null : adviceDesc.getInterceptors();
        if (interceptors != null) {
            onExit(context, interceptors, EMPTY_EXCEPTION_HANDLER);
        }
    }

    /**
     * Handles the exit point for a given execution context and a list of interceptors.
     *
     * @param <T>              the type of the execution context
     * @param context          the execution context
     * @param interceptors     the list of interceptors to be executed
     * @param exceptionHandler the handler for exceptions
     * @throws Throwable if any exception occurs during interception
     */
    public static <T extends ExecutableContext> void onExit(T context,
                                                            List<Interceptor> interceptors,
                                                            ExceptionHandler exceptionHandler) throws Throwable {
        if (context == null || interceptors == null)
            return;
        for (Interceptor interceptor : interceptors) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("exit [%s], interceptor is [%s].", context.getDescription(), interceptor.getClass().getName()));
            }
            if (context.isSuccess()) {
                handle(context, interceptor, Interceptor::onSuccess, exceptionHandler, "success");
            } else {
                handle(context, interceptor, Interceptor::onError, exceptionHandler, "recover");
            }
            handle(context, interceptor, Interceptor::onExit, exceptionHandler, "exit");
        }
    }

    /**
     * Generic method for handling interception actions.
     *
     * @param <T>          the type of the execution context
     * @param context      the execution context
     * @param interceptor  the interceptor to be executed
     * @param consumer     the action to be performed by the interceptor
     * @param errorHandler the handler for exceptions
     * @param action       the name of the action being performed
     * @throws Throwable if any exception occurs during interception
     */
    private static <T extends ExecutableContext> void handle(T context,
                                                             Interceptor interceptor,
                                                             BiConsumer<Interceptor, T> consumer,
                                                             ExceptionHandler errorHandler,
                                                             String action) throws Throwable {
        try {
            consumer.accept(interceptor, context);
        } catch (Throwable t) {
            logger.error(String.format("failed to %s %s, caused by %s", action, context.getDescription(), t.getMessage()), t);
            if (errorHandler != null) {
                try {
                    errorHandler.handle(context, interceptor, t);
                } catch (Throwable e) {
                    logger.error(String.format("failed to handle %s %s error, caused by %s", action, context.getDescription(), t.getMessage()), t);
                }
            } else {
                throw t;
            }
        }
    }

    /**
     * Retrieves or creates an AdviceDesc instance for the given advice key.
     *
     * @param adviceKey the unique key of the advice
     * @return the AdviceDesc instance
     */
    public static AdviceDesc getOrCreate(String adviceKey) {
        return advices.computeIfAbsent(adviceKey, AdviceDesc::new);
    }

    /**
     * Removes an advice identified by its unique key.
     *
     * @param adviceKey the unique key of the advice to be removed
     */
    public static void remove(String adviceKey) {
        advices.remove(adviceKey);
    }

}

