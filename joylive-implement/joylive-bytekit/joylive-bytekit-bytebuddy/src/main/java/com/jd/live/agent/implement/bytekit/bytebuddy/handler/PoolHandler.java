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
import com.jd.live.agent.core.config.EnhanceConfig;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.implement.bytekit.bytebuddy.BuilderHandler;
import com.jd.live.agent.implement.bytekit.bytebuddy.util.PoolCache;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.PoolStrategy;

import java.lang.instrument.Instrumentation;

/**
 * PoolHandler
 *
 * @since 1.6.0
 */
@Injectable
@Extension(value = "PoolHandler", order = BuilderHandler.ORDER_POOL_HANDLER)
@ConditionalOnProperty(value = "agent.enhance.poolEnabled", matchIfMissing = true)
public class PoolHandler implements BuilderHandler, ExtensionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PoolHandler.class);

    @Inject(EnhanceConfig.COMPONENT_ENHANCE_CONFIG)
    private EnhanceConfig enhanceConfig;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    private final PoolCache poolCache = new PoolCache(256);

    @Override
    public AgentBuilder configure(AgentBuilder builder, Instrumentation instrumentation) {
        return builder.with(new PoolStrategy.WithTypePoolCache.Simple(poolCache));
    }

    @Override
    public void initialize() {
        if (enhanceConfig.getPoolExpireTime() > 0) {
            addCleanTask();
        }
    }

    private void addCleanTask() {
        timer.delay("LiveAgent-ByteBuddy-Pool-Cleaner", enhanceConfig.getPoolCleanInterval(), () -> {
            int old = poolCache.size();
            poolCache.recycle(enhanceConfig.getPoolExpireTime());
            int current = poolCache.size();
            logger.info("Clean expired cache from byte buddy pool. " + old + " -> " + current);
            addCleanTask();
        });
    }
}
