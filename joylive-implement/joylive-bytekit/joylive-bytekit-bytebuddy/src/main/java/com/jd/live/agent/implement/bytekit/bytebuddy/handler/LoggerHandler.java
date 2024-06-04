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
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.implement.bytekit.bytebuddy.BuilderHandler;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.NeverNull;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;

/**
 * LoggerHandler
 *
 * @since 1.0.0
 */
@Extension(value = "LoggerHandler", order = BuilderHandler.ORDER_LOGGER_HANDLER)
@ConditionalOnProperty(value = "agent.enhance.loggerEnabled", matchIfMissing = true)
public class LoggerHandler implements BuilderHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoggerHandler.class);

    @Override
    public AgentBuilder configure(AgentBuilder builder, Instrumentation instrumentation) {
        return builder.with(new EventLogger());
    }

    /**
     * LogListener
     *
     * @since 1.0.0
     */
    protected static class EventLogger implements AgentBuilder.Listener {

        @Override
        public void onDiscovery(@NeverNull String typeName,
                                @MaybeNull ClassLoader classLoader,
                                @MaybeNull JavaModule module,
                                boolean loaded) {
            if (logger.isDebugEnabled()) {
                String message = String.format("[Byte Buddy] DISCOVERY %s [%s, %s, %s, loaded=%b]", typeName, classLoader, module, Thread.currentThread(), loaded);
                logger.debug(message);
            }
        }

        @Override
        public void onTransformation(@NeverNull TypeDescription typeDescription,
                                     @MaybeNull ClassLoader classLoader,
                                     @MaybeNull JavaModule module,
                                     boolean loaded,
                                     @NeverNull DynamicType dynamicType) {
            if (logger.isInfoEnabled()) {
                String message = String.format("[Byte Buddy] TRANSFORM %s [%s, %s, %s, loaded=%b]", typeDescription.getName(), classLoader, module, Thread.currentThread(), loaded);
                logger.info(message);
            }
        }

        @Override
        public void onIgnored(@NeverNull TypeDescription typeDescription,
                              @MaybeNull ClassLoader classLoader,
                              @MaybeNull JavaModule module,
                              boolean loaded) {
            if (logger.isDebugEnabled()) {
                String message = String.format("[Byte Buddy] IGNORE %s [%s, %s, %s, loaded=%b]", typeDescription.getName(), classLoader, module, Thread.currentThread(), loaded);
                logger.debug(message);
            }
        }

        @Override
        public void onError(@NeverNull String typeName,
                            @MaybeNull ClassLoader classLoader,
                            @MaybeNull JavaModule module,
                            boolean loaded,
                            @NeverNull Throwable throwable) {
            OutputStream bos = new ByteArrayOutputStream(1024);
            PrintStream printStream = new PrintStream(bos);
            printStream.printf("[Byte Buddy] ERROR %s [%s, %s, %s, loaded=%b]", typeName, classLoader, module, Thread.currentThread(), loaded);
            throwable.printStackTrace(printStream);
            logger.error(bos.toString());
        }

        @Override
        public void onComplete(@NeverNull String typeName,
                               @MaybeNull ClassLoader classLoader,
                               @MaybeNull JavaModule module,
                               boolean loaded) {
            if (logger.isDebugEnabled()) {
                String message = String.format("[Byte Buddy] COMPLETE %s [%s, %s, %s, loaded=%b]", typeName, classLoader, module, Thread.currentThread(), loaded);
                logger.debug(message);
            }
        }
    }
}
