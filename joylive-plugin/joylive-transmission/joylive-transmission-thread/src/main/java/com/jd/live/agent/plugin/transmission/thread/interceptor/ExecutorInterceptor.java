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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.thread.Camera;
import com.jd.live.agent.core.thread.Snapshot;
import com.jd.live.agent.plugin.transmission.thread.adapter.AbstractThreadAdapter;
import com.jd.live.agent.plugin.transmission.thread.adapter.CallableAdapter;
import com.jd.live.agent.plugin.transmission.thread.adapter.RunnableAdapter;
import com.jd.live.agent.plugin.transmission.thread.adapter.RunnableAndCallableAdapter;
import com.jd.live.agent.plugin.transmission.thread.config.ThreadConfig;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * ExecutorInterceptor
 */
public class ExecutorInterceptor extends InterceptorAdaptor {

    private static final String FIELD_CALLABLE = "callable";

    private final Field callableField;

    private final Camera[] cameras;

    private final ThreadConfig threadConfig;

    public ExecutorInterceptor(List<Camera> cameras, ThreadConfig threadConfig) {
        this.cameras = cameras == null ? new Camera[0] : cameras.toArray(new Camera[0]);
        this.threadConfig = threadConfig;
        this.callableField = getCallableField();
    }

    private Field getCallableField() {
        Field result = null;
        try {
            result = FutureTask.class.getDeclaredField(FIELD_CALLABLE);
            result.setAccessible(true);
        } catch (NoSuchFieldException ignore) {
        }
        return result;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object target = ctx.getTarget();
        String name = target.getClass().getSimpleName();
        Object[] arguments = ctx.getArguments();
        if (arguments == null || arguments.length == 0 || cameras.length == 0) {
            return;
        }
        Object argument = arguments[0];
        if (argument == null) {
            return;
        }
        Object unwrapped = unwrap(argument);
        if (unwrapped instanceof AbstractThreadAdapter) {
            return;
        }
        if (threadConfig.isExcludedTask(unwrapped.getClass().getName())) {
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
        if (argument instanceof AbstractThreadAdapter) {
            return argument;
        }
        if (argument instanceof FutureTask && callableField != null) {
            try {
                return callableField.get(argument);
            } catch (Exception ignore) {
            }
        }
        return argument;
    }

}