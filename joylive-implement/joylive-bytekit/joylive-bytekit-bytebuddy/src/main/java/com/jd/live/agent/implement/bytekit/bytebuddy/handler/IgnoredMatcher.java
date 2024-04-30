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
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/**
 * IgnoredMatcher
 *
 * @since 1.0.0
 */
public class IgnoredMatcher implements AgentBuilder.RawMatcher {

    private final EnhanceConfig config;

    public IgnoredMatcher(EnhanceConfig config) {
        this.config = config;
    }

    @Override
    public boolean matches(TypeDescription typeDescription,
                           ClassLoader classLoader,
                           JavaModule javaModule,
                           Class<?> aClass,
                           ProtectionDomain protectionDomain) {
        return isArray(typeDescription) || isPrimitive(typeDescription) || isAgent(classLoader) || isExcluded(typeDescription);
    }

    protected boolean isArray(TypeDescription typeDesc) {
        return typeDesc.isArray();
    }

    protected boolean isPrimitive(TypeDescription typeDesc) {
        return typeDesc.isPrimitive();
    }

    protected boolean isAgent(ClassLoader classLoader) {
        return classLoader instanceof LiveClassLoader;
    }

    protected boolean isExcluded(TypeDescription typeDesc) {
        return config.isExclude(typeDesc.getClass());
    }
}
