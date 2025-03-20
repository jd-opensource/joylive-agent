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
package com.jd.live.agent.plugin.transmission.thread.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.thread.Camera;
import com.jd.live.agent.core.thread.Snapshot;
import com.jd.live.agent.governance.config.TransmitConfig.ThreadConfig;
import com.jd.live.agent.plugin.transmission.thread.adapter.AbstractThreadAdapter;
import com.jd.live.agent.plugin.transmission.thread.adapter.CallableAdapter;
import com.jd.live.agent.plugin.transmission.thread.adapter.RunnableAdapter;
import com.jd.live.agent.plugin.transmission.thread.adapter.RunnableAndCallableAdapter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * ExecutorInterceptor
 */
public class ExecutorInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorInterceptor.class);

    private static final String FIELD_CALLABLE = "callable";

    private final Field callableField;

    private final Camera[] cameras;

    private final ThreadConfig threadConfig;

    private final Map<Class<?>, Boolean> excludes = new ConcurrentHashMap<>(128);

    public ExecutorInterceptor(List<Camera> cameras, ThreadConfig threadConfig) {
        this.cameras = cameras == null ? new Camera[0] : cameras.toArray(new Camera[0]);
        this.threadConfig = threadConfig;
        this.callableField = getCallableField();
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object target = ctx.getTarget();
        String name = target.getClass().getSimpleName();
        Object[] arguments = ctx.getArguments();
        if (arguments == null
                || arguments.length == 0
                || cameras.length == 0
                || isExcludedExecutor(target)
                || target instanceof ThreadPoolExecutor
                && isExcludedThreadFactory(((ThreadPoolExecutor) target).getThreadFactory())) {
            return;
        }
        Object argument = arguments[0];
        Object unwrapped = unwrap(argument);
        if (argument == null) {
            return;
        } else if (unwrapped instanceof AbstractThreadAdapter) {
            return;
        } else if (isExcludedTask(unwrapped)) {
            return;
        }

        Snapshot[] snapshots = new Snapshot[cameras.length];
        for (int i = 0; i < cameras.length; i++) {
            snapshots[i] = new Snapshot(cameras[i], cameras[i].snapshot());
        }
        if (argument instanceof Runnable && argument instanceof Callable) {
            arguments[0] = new RunnableAndCallableAdapter<>(name, (Runnable) argument, (Callable<?>) argument, snapshots);
        } else if (argument instanceof Runnable) {
            arguments[0] = new RunnableAdapter<>(name, (Runnable) argument, snapshots);
        } else if (argument instanceof Callable) {
            arguments[0] = new CallableAdapter<>(name, (Callable<?>) argument, snapshots);
        }
    }

    /**
     * Checks if the given thread factory is excluded by its class type.
     * The result is cached in the {@code excludes} map to avoid repeated computations.
     *
     * @param factory The thread factory object to check.
     * @return {@code true} if the thread factory is excluded, {@code false} otherwise.
     */
    private boolean isExcludedThreadFactory(ThreadFactory factory) {
        return factory != null && excludes.computeIfAbsent(factory.getClass(), this::isExcludedThreadFactoryType);
    }

    /**
     * Checks if the given thread factory type is excluded.
     * If the thread factory is excluded, logs an informational message.
     *
     * @param type The class type of the thread factory to check.
     * @return {@code true} if the thread factory type is excluded, {@code false} otherwise.
     */
    private boolean isExcludedThreadFactoryType(Class<?> type) {
        if (threadConfig.isExcludedTask(type)) {
            logger.info("Disable transmission in threads of factory {}@{} loaded by {}", type.getName(), System.identityHashCode(type), type.getClassLoader());
            return true;
        }
        logger.info("Enable transmission in threads of factory {}@{} loaded by {}", type.getName(), System.identityHashCode(type), type.getClassLoader());
        return false;
    }


    /**
     * Checks if the given task is excluded by its class type.
     * The result is cached in the {@code excludes} map to avoid repeated computations.
     *
     * @param task The task object to check.
     * @return {@code true} if the task is excluded, {@code false} otherwise.
     */
    private boolean isExcludedTask(Object task) {
        return task != null && excludes.computeIfAbsent(task.getClass(), this::isExcludeTaskType);
    }

    /**
     * Checks if the given task type is excluded.
     * If the task is excluded, logs an informational message.
     *
     * @param type The class type of the task to check.
     * @return {@code true} if the task type is excluded, {@code false} otherwise.
     */
    private boolean isExcludeTaskType(Class<?> type) {
        if (threadConfig.isExcludedTask(type)) {
            logger.info("Disable transmission in task {}@{} loaded by {}", type.getName(), System.identityHashCode(type), type.getClassLoader());
            return true;
        }
        logger.info("Enable transmission in task {}@{} loaded by {}", type.getName(), System.identityHashCode(type), type.getClassLoader());
        return false;
    }

    /**
     * Checks if the given executor is excluded by its class type.
     * The result is cached in the {@code excludes} map to avoid repeated computations.
     *
     * @param executor The executor object to check.
     * @return {@code true} if the executor is excluded, {@code false} otherwise.
     */
    private boolean isExcludedExecutor(Object executor) {
        return executor != null && excludes.computeIfAbsent(executor.getClass(), this::isExcludeExecutorType);
    }

    /**
     * Checks if the given executor type is excluded.
     * If the executor is excluded, logs an informational message.
     *
     * @param type The class type of the executor to check.
     * @return {@code true} if the executor type is excluded, {@code false} otherwise.
     */
    private boolean isExcludeExecutorType(Class<?> type) {
        if (threadConfig.isExcludedExecutor(type)) {
            logger.info("Disable transmission in executor {}@{} loaded by {}", type.getName(), System.identityHashCode(type), type.getClassLoader());
            return true;
        }
        logger.info("Enable transmission in executor {}@{} loaded by {}", type.getName(), System.identityHashCode(type), type.getClassLoader());
        return false;
    }

    /**
     * Unwraps the provided argument object to retrieve its underlying value. If the argument is an instance
     * of {@link AbstractThreadAdapter}, it is returned directly. If the argument is an instance of {@link FutureTask},
     * the method attempts to extract the 'callable' field from it using reflection. If the extraction fails,
     * the exception is ignored, and the original argument is returned.
     *
     * @param argument the object to unwrap, which could be an instance of {@link AbstractThreadAdapter} or
     *                 {@link FutureTask} or any other Object.
     * @return the unwrapped object if unwrapping is possible, otherwise the original object.
     */
    private Object unwrap(Object argument) {
        if (argument == null) {
            return null;
        } else if (argument instanceof AbstractThreadAdapter) {
            return argument;
        } else if (argument instanceof FutureTask && callableField != null) {
            try {
                return callableField.get(argument);
            } catch (Exception ignore) {
            }
        }
        return argument;
    }

    /**
     * Retrieves the {@link Field} object representing the {@code callable} field in the {@link FutureTask} class.
     * This method uses reflection to access the private field and makes it accessible.
     *
     * @return The {@link Field} object representing the {@code callable} field, or {@code null} if the field is not found.
     */
    private static Field getCallableField() {
        Field result = null;
        try {
            result = FutureTask.class.getDeclaredField(FIELD_CALLABLE);
            result.setAccessible(true);
        } catch (NoSuchFieldException ignore) {
        }
        return result;
    }

}