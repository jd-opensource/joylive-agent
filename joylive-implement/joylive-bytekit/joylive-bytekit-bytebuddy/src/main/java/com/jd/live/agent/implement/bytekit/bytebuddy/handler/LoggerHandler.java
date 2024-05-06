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
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;

/**
 * LoggerHandler
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "LoggerHandler", order = BuilderHandler.ORDER_LOGGER_HANDLER)
@ConditionalOnProperty(value = "agent.enhance.loggerEnabled", matchIfMissing = true)
public class LoggerHandler implements BuilderHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoggerHandler.class);

    @Inject(value = Publisher.ENHANCE)
    private Publisher<AgentEvent> publisher;

    @Override
    public AgentBuilder configure(AgentBuilder builder, Instrumentation instrumentation) {
        return builder.with(new EventLogger(publisher));
    }

    /**
     * LogListener
     *
     * @since 1.0.0
     */
    protected static class EventLogger implements AgentBuilder.Listener {

        private final Publisher<AgentEvent> publisher;

        public EventLogger(Publisher<AgentEvent> publisher) {
            this.publisher = publisher;
        }

        @Override
        public void onDiscovery(String typeName, @MaybeNull ClassLoader classLoader, @MaybeNull JavaModule module, boolean loaded) {
            if (logger.isDebugEnabled()) {
                String message = String.format("[Byte Buddy] DISCOVERY %s [%s, %s, %s, loaded=%b]", typeName, classLoader, module, Thread.currentThread(), loaded);
                logger.debug(message);
            }
        }

        @Override
        public void onTransformation(TypeDescription typeDescription, @MaybeNull ClassLoader classLoader, @MaybeNull JavaModule module, boolean loaded, DynamicType dynamicType) {
            if (logger.isInfoEnabled()) {
                String message = String.format("[Byte Buddy] TRANSFORM %s [%s, %s, %s, loaded=%b]", typeDescription.getName(), classLoader, module, Thread.currentThread(), loaded);
                publisher.offer(new Event<>(new AgentEvent(AgentEvent.EventType.AGENT_ENHANCE_SUCCESS, message)));
                logger.info(message);
            }
        }

        @Override
        public void onIgnored(TypeDescription typeDescription, @MaybeNull ClassLoader classLoader, @MaybeNull JavaModule module, boolean loaded) {
            if (logger.isDebugEnabled()) {
                String message = String.format("[Byte Buddy] IGNORE %s [%s, %s, %s, loaded=%b]", typeDescription.getName(), classLoader, module, Thread.currentThread(), loaded);
                logger.debug(message);
            }
        }

        @Override
        public void onError(String typeName, @MaybeNull ClassLoader classLoader, @MaybeNull JavaModule module, boolean loaded, Throwable throwable) {
            OutputStream bos = new ByteArrayOutputStream(1024);
            PrintStream printStream = new PrintStream(bos);
            printStream.printf("[Byte Buddy] ERROR %s [%s, %s, %s, loaded=%b]", typeName, classLoader, module, Thread.currentThread(), loaded);
            throwable.printStackTrace(printStream);
            String message = bos.toString();
            publisher.offer(new Event<>(new AgentEvent(AgentEvent.EventType.AGENT_ENHANCE_FAILURE, message)));
            logger.error(message);
        }

        @Override
        public void onComplete(String typeName, @MaybeNull ClassLoader classLoader, @MaybeNull JavaModule module, boolean loaded) {
            if (logger.isDebugEnabled()) {
                String message = String.format("[Byte Buddy] COMPLETE %s [%s, %s, %s, loaded=%b]", typeName, classLoader, module, Thread.currentThread(), loaded);
                logger.debug(message);
            }
        }
    }
}
