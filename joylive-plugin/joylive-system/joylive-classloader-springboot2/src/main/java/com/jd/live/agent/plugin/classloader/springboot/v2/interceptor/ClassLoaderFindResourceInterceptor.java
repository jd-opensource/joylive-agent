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
package com.jd.live.agent.plugin.classloader.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.classloader.Resourcer;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.ClassLoaderConfig;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;

import java.net.URL;

public class ClassLoaderFindResourceInterceptor extends InterceptorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderFindResourceInterceptor.class);

    private final Resourcer resourcer;

    private final ClassLoaderConfig classLoaderConfig;

    public ClassLoaderFindResourceInterceptor(Resourcer resourcer, ClassLoaderConfig classLoaderConfig) {
        this.resourcer = resourcer;
        this.classLoaderConfig = classLoaderConfig;
    }

    /**
     * Enhanced logic after method successfully execute
     *
     * @param ctx ExecutableContext
     * @see <code>org.springframework.boot.loader.LaunchedURLClassLoader#findResource</code>
     */
    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        if (mc.getResult() == null) {
            String name = (String) mc.getArguments()[0];
            String path = name.replace('/', '.');
            if (classLoaderConfig.isEssential(path)) {
                URL url = resourcer.findResource(name);
                if (url != null) {
                    mc.setResult(url);
                    logger.info("successfully find resource " + name);
                } else {
                    logger.warn("failed to find resource " + name);
                }
            }
        }
    }
}
