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
package com.jd.live.agent.plugin.registry.springcloud.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;
import org.springframework.context.ApplicationContextAware;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * FeignClientFactoryBeanInterceptor
 */
public class FeignClientFactoryBeanInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(FeignClientFactoryBeanInterceptor.class);

    private final Registry registry;

    public FeignClientFactoryBeanInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // FeignClientFactoryBean is package-private in 2.2.6-, so we can't direct access it.
        String name = Accessor.getName(ctx.getTarget());
        if (name != null && !registry.isSubscribed(name)) {
            registry.subscribe(name);
            logger.info("Found feign client consumer, service: {}", name);
        }
    }

    private static class Accessor {

        private static final Class<?> type = loadClass("org.springframework.cloud.openfeign.FeignClientFactoryBean",
                ApplicationContextAware.class.getClassLoader());
        private static final FieldAccessor name = FieldAccessorFactory.getAccessor(type, "name");

        public static String getName(Object bean) {
            return name.get(bean, String.class);
        }
    }
}
