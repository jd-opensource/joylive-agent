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
package com.jd.live.agent.plugin.router.springgateway.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.http.HttpUtils;

import java.net.URI;

/**
 * GatewayRouteConstructorInterceptor
 */
public class GatewayRouteDefInterceptor extends InterceptorAdaptor {

    @Override
    public void onEnter(ExecutableContext ctx) {
        URI uri = ctx.getArgument(0);
        if (uri != null && uri.getHost() == null && uri.getAuthority() != null && "lb".equalsIgnoreCase(uri.getScheme())) {
            // try fixing special service name. such as "lb://SleepService:DEFAULT"
            String string = uri.toString();
            com.jd.live.agent.core.util.URI u = com.jd.live.agent.core.util.URI.parse(string);
            if (u != null && u.getHost() != null) {
                ctx.setArgument(0, HttpUtils.newURI(uri, uri.getScheme(), null,
                        u.getHost(), null, null, null, null, string));
            }
        }
    }
}
