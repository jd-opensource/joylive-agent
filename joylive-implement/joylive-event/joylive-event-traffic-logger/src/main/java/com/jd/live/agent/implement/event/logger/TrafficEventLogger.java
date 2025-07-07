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
import com.jd.live.agent.core.event.EventHandler.EventProcessor;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.event.TrafficEvent.ActionType;

@Extension("TrafficEventLogger")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_CIRCUIT_BREAK_LOG_ENABLED)
public class TrafficEventLogger implements Subscription<TrafficEvent>, EventProcessor<TrafficEvent> {

    private static final Logger logger = LoggerFactory.getLogger(TrafficEventLogger.class);

    @Override
    public void process(TrafficEvent event) {
        if (event.getActionType() == ActionType.REJECT) {
            switch (event.getRejectType()) {
                case REJECT_CIRCUIT_BREAK:
                    logger.error("Circuit break {} requests, service={}, path={}, method={} ",
                            event.getRequests(), event.getService(), event.getPath(), event.getMethod());
                    break;
                case REJECT_DEGRADE:
                    logger.error("Degrade {} requests, service={}, path={}, method={} ",
                            event.getRequests(), event.getService(), event.getPath(), event.getMethod());
                    break;
            }
        }
    }

    @Override
    public String getTopic() {
        return Publisher.TRAFFIC;
    }
}
