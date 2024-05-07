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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.context.AgentPath;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.implement.bytekit.bytebuddy.BuilderHandler;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.NeverNull;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ExporterHandler
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "ExporterHandler", order = BuilderHandler.ORDER_EXPORTER_HANDLER)
@ConditionalOnProperty(value = "agent.enhance.exporterEnabled")
public class ExporterHandler implements BuilderHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExporterHandler.class);

    @Inject(value = AgentPath.COMPONENT_AGENT_PATH)
    private AgentPath agentPath;

    @Override
    public AgentBuilder configure(AgentBuilder builder, Instrumentation instrumentation) {
        File output = new File(agentPath.getOutputPath(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + File.separator);
        if (!output.mkdirs() && !output.exists()) {
            logger.warn("failed to create output directory " + output.getPath());
            return builder;
        } else {
            return builder.with(new Exporter(output));
        }
    }

    /**
     * Exporter
     *
     * @since 1.0.0
     */
    protected static class Exporter extends AgentBuilder.Listener.Adapter {

        private final File output;

        public Exporter(File output) {
            this.output = output;
        }

        @Override
        public void onTransformation(@NeverNull TypeDescription typeDescription,
                                     @MaybeNull ClassLoader classLoader,
                                     @MaybeNull JavaModule javaModule,
                                     boolean b,
                                     @NeverNull DynamicType dynamicType) {
            try {
                dynamicType.saveIn(output);
            } catch (IOException e) {
                logger.warn("failed to save class byte code. " + typeDescription.getTypeName());
            }
        }
    }
}
