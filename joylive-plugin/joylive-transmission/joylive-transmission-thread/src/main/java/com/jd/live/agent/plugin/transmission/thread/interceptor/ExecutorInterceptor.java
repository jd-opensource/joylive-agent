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

import java.util.List;
import java.util.concurrent.Callable;

/**
 * ExecutorInterceptor
 */
public class ExecutorInterceptor extends InterceptorAdaptor {

    private final Camera[] cameras;

    public ExecutorInterceptor(List<Camera> cameras) {
        this.cameras = cameras == null ? new Camera[0] : cameras.toArray(new Camera[0]);
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
        if (argument == null || argument instanceof AbstractThreadAdapter) {
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

}