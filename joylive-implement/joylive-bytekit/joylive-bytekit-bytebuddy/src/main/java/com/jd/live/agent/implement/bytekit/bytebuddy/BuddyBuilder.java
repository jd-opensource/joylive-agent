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
package com.jd.live.agent.implement.bytekit.bytebuddy;

import com.jd.live.agent.core.bytekit.ByteBuilder;
import com.jd.live.agent.core.bytekit.transformer.Resetter;
import com.jd.live.agent.core.plugin.definition.PluginDeclare;
import com.jd.live.agent.implement.bytekit.bytebuddy.plugin.PluginTransformHandler;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * BuddyBuilder
 *
 * @since 1.0.0
 */
public class BuddyBuilder implements ByteBuilder {

    private final List<BuilderHandler> handlers;

    public BuddyBuilder(List<BuilderHandler> handlers) {
        this.handlers = handlers == null ? new ArrayList<>() : new ArrayList<>(handlers);
    }

    @Override
    public Resetter install(Instrumentation instrumentation) {
        AgentBuilder agentBuilder = new AgentBuilder.Default().disableClassFormatChanges();
        for (BuilderHandler handler : handlers) {
            agentBuilder = handler.configure(agentBuilder, instrumentation);
        }
        ResettableClassFileTransformer transformer = agentBuilder.installOn(instrumentation);
        return new TransformerResetter(transformer, instrumentation);
    }

    @Override
    public ByteBuilder append(PluginDeclare plugin) {
        if (plugin != null) {
            handlers.add(new PluginTransformHandler(plugin));
        }
        return this;
    }

    private static class TransformerResetter implements Resetter {

        private final ResettableClassFileTransformer transformer;

        private final Instrumentation instrumentation;

        TransformerResetter(ResettableClassFileTransformer transformer, Instrumentation instrumentation) {
            this.transformer = transformer;
            this.instrumentation = instrumentation;
        }

        @Override
        public void reset() {
            transformer.reset(instrumentation,
                    AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                    AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE,
                    AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE,
                    AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemOut());
        }
    }
}
