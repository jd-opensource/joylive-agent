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
package com.jd.live.agent.implement.bytekit.bytebuddy.test.plugin;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;
import com.jd.live.agent.core.bytekit.matcher.ElementMatcher;
import com.jd.live.agent.core.bytekit.type.MethodDesc;
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.implement.bytekit.bytebuddy.test.Foo;

import java.util.Objects;

/**
 * TestPlugin
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extension(value = "test")
public class TestPlugin implements PluginDefinition {

    private static final Logger logger = LoggerFactory.getLogger(TestPlugin.class);

    @Override
    public ElementMatcher<TypeDesc> getMatcher() {
        return target -> Objects.equals(target.getActualName(), Foo.class.getName());
    }

    @Override
    public InterceptorDefinition[] getInterceptors() {
        return new InterceptorDefinition[]{
                new InterceptorDefinition() {
                    @Override
                    public ElementMatcher<MethodDesc> getMatcher() {
                        return target -> Objects.equals(target.getActualName(), "say");
                    }

                    @Override
                    public Interceptor getInterceptor() {
                        return new TestInterceptor();
                    }
                }
        };
    }
}
