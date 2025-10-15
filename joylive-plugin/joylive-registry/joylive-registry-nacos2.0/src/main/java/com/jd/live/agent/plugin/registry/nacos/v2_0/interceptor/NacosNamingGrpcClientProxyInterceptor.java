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
package com.jd.live.agent.plugin.registry.nacos.v2_0.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.registry.nacos.v2_0.util.SecurityContext;

/**
 * Nacos Naming Client Proxy Interceptor.
 *
 * <p>This interceptor handles authentication exceptions by automatically
 * refreshing the authentication token when a NO_RIGHT error occurs.
 * It intercepts authentication failures and triggers a re-login process
 * to obtain a new valid token.</p>
 */
public class NacosNamingGrpcClientProxyInterceptor extends InterceptorAdaptor {

    @Override
    public void onError(ExecutableContext ctx) {
        if (SecurityContext.isNoRight(ctx.getThrowable())) {
            // refresh token
            SecurityContext.reLogin(ctx.getTarget());
        }
    }

}
