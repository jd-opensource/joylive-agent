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
import com.jd.live.agent.core.util.http.HttpUtils;
import org.springframework.web.util.UriComponents;

import java.net.URISyntaxException;

import static com.jd.live.agent.core.Constants.PREDICATE_LB;

/**
 * HierarchicalUriComponentsInterceptor
 */
public class HierarchicalUriComponentsInterceptor extends InterceptorAdaptor {

    @Override
    public void onExit(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        if (mc.getThrowable() != null) {
            if (mc.getThrowable() instanceof URISyntaxException || mc.getThrowable().getCause() instanceof URISyntaxException) {
                UriComponents components = (UriComponents) mc.getTarget();
                if (PREDICATE_LB.test(components.getScheme())) {
                    // try fixing special service name. such as "lb://SleepService:DEFAULT"
                    mc.setThrowable(null);
                    mc.setResult(HttpUtils.newURI(
                            null,
                            components.getScheme(),
                            components.getUserInfo(),
                            components.getHost(),
                            components.getPort(),
                            components.getPath(),
                            components.getQuery(),
                            components.getFragment(),
                            components.toString()));
                }
            }
        }
    }
}
