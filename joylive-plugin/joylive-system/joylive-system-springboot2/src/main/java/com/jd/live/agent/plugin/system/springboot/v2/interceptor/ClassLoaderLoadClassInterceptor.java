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
package com.jd.live.agent.plugin.system.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.classloader.Resourcer;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.ClassLoaderConfig;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;

public class ClassLoaderLoadClassInterceptor extends InterceptorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderLoadClassInterceptor.class);

    private final Resourcer resourcer;

    private final ClassLoaderConfig classLoaderConfig;

    public ClassLoaderLoadClassInterceptor(Resourcer resourcer, ClassLoaderConfig classLoaderConfig) {
        this.resourcer = resourcer;
        this.classLoaderConfig = classLoaderConfig;
    }

    @Override
    public void onError(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        String name = (String) ctx.getArguments()[0];
        if (classLoaderConfig.isEssential(name)) {
            try {
                mc.success(resourcer.loadClass(name));
                logger.info("successfully loading class " + name);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                logger.warn("failed to load class " + name);
            }
        }
    }
}
