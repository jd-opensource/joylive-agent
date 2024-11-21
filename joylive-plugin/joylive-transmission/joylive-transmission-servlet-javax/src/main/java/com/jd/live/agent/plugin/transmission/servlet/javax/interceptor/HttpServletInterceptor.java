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
package com.jd.live.agent.plugin.transmission.servlet.javax.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * An interceptor for HttpServlet requests to capture and restore context (cargo) from the request headers.
 * This is useful for maintaining request-specific context across different components of a distributed system.
 */
public class HttpServletInterceptor extends InterceptorAdaptor {

    private static final ThreadLocal<Long> LOCK = new ThreadLocal<>();

    private final CargoRequire require;

    public HttpServletInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    /**
     * Enhanced logic before method execution
     *
     * @param ctx ExecutableContext
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        if (LOCK.get() == null) {
            LOCK.set(ctx.getId());
            restoreTag((HttpServletRequest) ctx.getArguments()[0]);
        }
    }

    /**
     * Restores cargo from the request headers into the RequestContext.
     *
     * @param request The incoming HttpServletRequest.
     */
    private void restoreTag(HttpServletRequest request) {
        RequestContext.create().addCargo(require, request.getHeaderNames(), request::getHeaders);
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        if (LOCK.get() == ctx.getId()) {
            LOCK.remove();
        }
    }
}
