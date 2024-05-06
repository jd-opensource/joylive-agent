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
package com.jd.live.agent.implement.bytekit.bytebuddy.plugin;

import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.plugin.definition.PluginDeclare;
import com.jd.live.agent.implement.bytekit.bytebuddy.BuilderHandler;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

/**
 * TransformHandler
 *
 * @since 1.0.0
 */
public class PluginTransformHandler implements BuilderHandler {

    private final PluginDeclare declare;

    private final ConditionMatcher conditionMatcher;

    public PluginTransformHandler(PluginDeclare declare, ConditionMatcher conditionMatcher) {
        this.declare = declare;
        this.conditionMatcher = conditionMatcher;
    }

    @Override
    public AgentBuilder configure(AgentBuilder builder, Instrumentation instrumentation) {
        PluginTransformer transformer = new PluginTransformer(declare, conditionMatcher);
        return builder.type(transformer).transform(transformer);
    }
}
