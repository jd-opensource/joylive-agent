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
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.extension.annotation.Extension;

import java.util.List;

@Extension("EventLogger")
public class EventLogger implements Subscription<AgentEvent> {
    private static final Logger logger = LoggerFactory.getLogger(EventLogger.class);

    @Override
    public void handle(List<Event<AgentEvent>> events) {
        if (events != null) {
            for (Event<AgentEvent> event : events) {
                logger.info(event.getData().getMessage());
            }
        }
    }

    @Override
    public String getTopic() {
        return Publisher.SYSTEM;
    }
}
