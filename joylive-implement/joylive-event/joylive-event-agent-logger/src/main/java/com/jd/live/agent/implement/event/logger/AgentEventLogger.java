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
package com.jd.live.agent.implement.event.logger;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.EventHandler.EventProcessor;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.Subscriber;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.extension.annotation.Extension;

@Extension("AgentEventLogger")
public class AgentEventLogger implements Subscriber {

    private static final Logger logger = LoggerFactory.getLogger(AgentEventLogger.class);

    @Override
    public Subscription<?>[] subscribe() {
        return new Subscription[]{AgentEventSubscription.INSTANCE};
    }

    private static class AgentEventSubscription implements Subscription<AgentEvent>, EventProcessor<AgentEvent> {

        private static final AgentEventSubscription INSTANCE = new AgentEventSubscription();

        @Override
        public void process(AgentEvent event) {
            logger.info(event.getMessage());
        }

        @Override
        public String getTopic() {
            return Publisher.SYSTEM;
        }
    }
}
