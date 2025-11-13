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
package com.jd.live.agent.plugin.router.springweb.v6.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.router.springweb.v6.util.CloudUtils;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.List;

/**
 * Intercepts router function building to cache error handlers.
 * <p>
 * This interceptor captures exception handling functions from router methods
 * and combines them into a single handler chain. The combined handler is cached
 * for subsequent invocations, allowing centralized error handling across routes.
 */
public class ReactiveRouterFunctionBuilderInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        RouterFunction<ServerResponse> routerFunction = mc.getResult();
        List<HandlerFilterFunction<ServerResponse, ServerResponse>> errorFunctions = CloudUtils.getErrorHandlers(mc.getTarget());
        if (errorFunctions != null && !errorFunctions.isEmpty()) {
            HandlerFilterFunction<ServerResponse, ServerResponse> combinedErrorFunction = null;
            for (HandlerFilterFunction<ServerResponse, ServerResponse> errorFunction : errorFunctions) {
                if (combinedErrorFunction == null) {
                    combinedErrorFunction = errorFunction;
                } else {
                    combinedErrorFunction = combinedErrorFunction.andThen(errorFunction);
                }
            }
            CloudUtils.putErrorFunction(routerFunction, combinedErrorFunction);
            CloudUtils.putErrorFunction(CloudUtils.getReactiveFilterFunction(routerFunction), combinedErrorFunction);
        }
    }
}
