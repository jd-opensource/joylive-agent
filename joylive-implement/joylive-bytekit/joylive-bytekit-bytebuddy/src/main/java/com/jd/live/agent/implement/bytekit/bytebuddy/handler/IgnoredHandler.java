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
package com.jd.live.agent.implement.bytekit.bytebuddy.handler;

import com.jd.live.agent.bootstrap.classloader.LiveClassLoader;
import com.jd.live.agent.core.config.EnhanceConfig;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.implement.bytekit.bytebuddy.BuilderHandler;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.NeverNull;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * IgnoredHandler
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "IgnoredHandler", order = BuilderHandler.ORDER_IGNORED_HANDLER)
public class IgnoredHandler implements BuilderHandler {

    @Inject(EnhanceConfig.COMPONENT_ENHANCE_CONFIG)
    private EnhanceConfig enhanceConfig;

    @Override
    public AgentBuilder configure(AgentBuilder builder, Instrumentation instrumentation) {
        return builder.ignore(new IgnoredMatcher(enhanceConfig));
    }

    /**
     * IgnoredMatcher
     *
     * @since 1.0.0
     */
    protected static class IgnoredMatcher implements AgentBuilder.RawMatcher {

        private final EnhanceConfig config;

        public IgnoredMatcher(EnhanceConfig config) {
            this.config = config;
        }

        @Override
        public boolean matches(@NeverNull TypeDescription description,
                               @MaybeNull ClassLoader classLoader,
                               @MaybeNull JavaModule javaModule,
                               @MaybeNull Class<?> aClass,
                               @MaybeNull ProtectionDomain protectionDomain) {
            return isArray(description)
                    || isPrimitive(description)
                    || isAgent(description, classLoader)
                    || isExcluded(description, classLoader)
                    || isReflectionDynamicCreated(description);
        }

        protected boolean isArray(TypeDescription description) {
            return description.isArray();
        }

        protected boolean isPrimitive(TypeDescription description) {
            return description.isPrimitive();
        }

        protected boolean isAgent(TypeDescription description, ClassLoader classLoader) {
            return classLoader instanceof LiveClassLoader || description.getActualName().startsWith("com.jd.live.agent.");
        }

        protected boolean isExcluded(TypeDescription description, ClassLoader classLoader) {
            return config.isExclude(description.getActualName(), classLoader);
        }

        protected boolean isReflectionDynamicCreated(TypeDescription description) {
            // jdk.internal.reflect.GeneratedMethodAccessor
            // jdk.internal.reflect.GeneratedConstructorAccessor
            return description.getActualName().startsWith("jdk.internal.reflect.Generated");
        }
    }
}
