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

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.implement.bytekit.bytebuddy.BuilderHandler;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

/**
 * RetransformHandler
 *
 * @since 1.0.0
 */
@Extension(value = "RetransformHandler", order = BuilderHandler.ORDER_RETRANSFORM_HANDLER)
@ConditionalOnProperty(value = "agent.enhance.retransformEnabled", matchIfMissing = true)
public class RetransformHandler implements BuilderHandler {

    @Override
    public AgentBuilder configure(AgentBuilder builder, Instrumentation instrumentation) {
        return builder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
    }
}
