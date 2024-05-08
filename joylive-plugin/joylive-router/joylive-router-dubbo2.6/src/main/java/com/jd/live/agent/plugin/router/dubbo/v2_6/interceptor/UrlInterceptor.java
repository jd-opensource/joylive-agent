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
package com.jd.live.agent.plugin.router.dubbo.v2_6.interceptor;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;

/**
 * UrlInterceptor
 */
public class UrlInterceptor extends InterceptorAdaptor {

    /**
     * Enhanced logic before method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see URL#getMethodParameter(String, String, int)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = mc.getArguments();
        if (Constants.RETRIES_KEY.equals(arguments[1])) {
            mc.setResult(0);
            mc.setSkip(true);
        }
    }

}
