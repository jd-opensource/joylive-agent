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
package com.jd.live.agent.governance.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.bootstrap.AgentLifecycle;
import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;

/**
 * This interceptor delays the execution of methods until the application is ready.
 * It hooks into the application's lifecycle and ensures that methods are only executed
 * when the application status is READY.
 */
public abstract class ReadyExecutionInterceptor extends InterceptorAdaptor {

    protected final Application application;

    protected final AgentLifecycle lifecycle;

    public ReadyExecutionInterceptor(Application application, AgentLifecycle lifecycle) {
        this.application = application;
        this.lifecycle = lifecycle;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        if (application.getStatus() == AppStatus.STARTING && isDelay(mc)) {
            beforeAddReadyHook(mc);
            lifecycle.addReadyHook(() -> {
                beforeExecute(mc);
                return mc.invoke();
            });
            mc.setSkip(true);
        }
    }

    /**
     * Determines if the method context should be delayed.
     *
     * @param ctx the method context
     * @return true if the method should be delayed, false otherwise
     */
    protected boolean isDelay(MethodContext ctx) {
        return true;
    }

    /**
     * Hook method executed before adding a ready hook.
     *
     * @param ctx the method context
     */
    protected void beforeAddReadyHook(MethodContext ctx) {

    }

    /**
     * Hook method executed before invoking the delayed method.
     *
     * @param ctx the method context
     */
    protected void beforeExecute(MethodContext ctx) {

    }

}
