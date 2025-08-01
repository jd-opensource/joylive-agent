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
import com.jd.live.agent.core.event.Subscriber;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.event.TrafficEvent.ActionType;

@Injectable
@Extension("TrafficEventLogger")
@ConditionalOnFlowControlEnabled
@ConditionalOnProperty(GovernanceConfig.CONFIG_CIRCUIT_BREAKER_LOGGING_ENABLED)
public class TrafficEventLogger implements Subscriber {

    private static final Logger logger = LoggerFactory.getLogger(TrafficEventLogger.class);

    @Override
    public Subscription<?>[] subscribe() {
        return new Subscription[]{TrafficEventSubscription.INSTANCE};
    }

    private static class TrafficEventSubscription implements Subscription<TrafficEvent>, EventProcessor<TrafficEvent> {

        private static final TrafficEventSubscription INSTANCE = new TrafficEventSubscription();

        @Override
        public void process(TrafficEvent event) {
            if (event.getActionType() == ActionType.REJECT) {
                switch (event.getRejectType()) {
                    case REJECT_CIRCUIT_BREAK:
                        log("CircuitBreak", event.getRequests(), event.getService(), event.getPath(), event.getMethod(), event.getGroup());
                        break;
                    case REJECT_DEGRADE:
                        log("Degrade", event.getRequests(), event.getService(), event.getPath(), event.getMethod(), event.getGroup());
                        break;
                }
            }
        }

        @Override
        public String getTopic() {
            return Publisher.TRAFFIC;
        }

        /**
         * Logs service request errors with formatted URL patterns.
         *
         * @param type     Error type/category
         * @param requests Number of failed requests
         * @param service  Service name/identifier
         * @param path     Request path (nullable)
         * @param method   Request method
         * @param group    Service group (nullable)
         */
        private void log(String type, int requests, String service, String path, String method, String group) {
            if (path == null || path.isEmpty() || path.equals("/")) {
                if (group == null || group.isEmpty()) {
                    if (method == null || method.isEmpty()) {
                        logger.error("{} {} requests, policy on service://{}", type, requests, service);
                    } else {
                        logger.error("{} {} requests, policy on service://{}?method={}", type, requests, service, method);
                    }
                } else if (method == null || method.isEmpty()) {
                    logger.error("{} {} requests, policy on service://{}?group={}", type, requests, service, group);
                } else {
                    logger.error("{} {} requests, policy on service://{}?group={}&method={}", type, requests, service, group, method);
                }
            } else if (group == null || group.isEmpty()) {
                if (method == null || method.isEmpty()) {
                    logger.error("{} {} requests, policy on service://{}/{}", type, requests, service, path);
                } else {
                    logger.error("{} {} requests, policy on service://{}/{}?method={}", type, requests, service, path, method);
                }
            } else if (method == null || method.isEmpty()) {
                logger.error("{} {} requests, policy on service://{}/{}?group={}", type, requests, service, path, group);
            } else {
                logger.error("{} {} requests, policy on service://{}/{}?group={}&method={}", type, requests, service, path, group, method);
            }
        }
    }
}
