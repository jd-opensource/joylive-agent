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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bytekit.ByteBuilder;
import com.jd.live.agent.core.bytekit.transformer.Resetter;
import com.jd.live.agent.core.config.EnhanceConfig;
import com.jd.live.agent.core.context.AgentPath;
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.plugin.definition.PluginDeclare;
import com.jd.live.agent.implement.bytekit.bytebuddy.handler.EventLogger;
import com.jd.live.agent.implement.bytekit.bytebuddy.handler.Exporter;
import com.jd.live.agent.implement.bytekit.bytebuddy.handler.IgnoredMatcher;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * BuddyBuilder
 *
 * @since 1.0.0
 */
public class BuddyBuilder implements ByteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(BuddyBuilder.class);

    private final AgentPath agentPath;

    private final Publisher<AgentEvent> publisher;

    private final EnhanceConfig enhanceConfig;

    private final ConditionMatcher conditionMatcher;

    private final List<BuilderHandler> handlers = new ArrayList<>();

    public BuddyBuilder(EnhanceConfig enhanceConfig,
                        AgentPath agentPath,
                        Publisher<AgentEvent> publisher,
                        ConditionMatcher conditionMatcher) {
        this.agentPath = agentPath;
        this.publisher = publisher;
        this.enhanceConfig = enhanceConfig;
        this.conditionMatcher = conditionMatcher;
    }

    @Override
    public Resetter install(Instrumentation instrumentation) {
        AgentBuilder agentBuilder = new AgentBuilder.Default().disableClassFormatChanges();
        for (BuilderHandler handler : handlers) {
            agentBuilder = handler.process(agentBuilder);
        }
        ResettableClassFileTransformer transformer = agentBuilder.installOn(instrumentation);
        return new TransformerResetter(transformer, instrumentation);
    }

    @Override
    public ByteBuilder append(PluginDeclare plugin) {
        if (plugin != null) {
            final PluginTransformer transformer = new PluginTransformer(plugin, conditionMatcher);
            handlers.add(builder -> builder.type(transformer).transform(transformer));
        }
        return this;
    }

    protected BuddyBuilder createBootstrapHandler() {
        if (enhanceConfig.isReTransformEnabled()) {
            handlers.add(builder -> builder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION));
        }
        return this;
    }

    protected BuddyBuilder createIgnoreHandler() {
        handlers.add(builder -> builder.ignore(new IgnoredMatcher(enhanceConfig)));
        return this;
    }

    protected BuddyBuilder createLogHandler() {
        if (enhanceConfig.isLogEnhance()) {
            handlers.add(builder -> builder.with(new EventLogger(publisher)));
        }
        return this;
    }

    protected BuddyBuilder createOutputHandler() {
        if (enhanceConfig.isOutputEnhance()) {
            File output = new File(agentPath.getOutputPath(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + File.separator);
            if (!output.mkdirs() && !output.exists()) {
                logger.warn("failed to create output directory " + output.getPath());
            } else {
                handlers.add(builder -> builder.with(new Exporter(output)));
            }
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
