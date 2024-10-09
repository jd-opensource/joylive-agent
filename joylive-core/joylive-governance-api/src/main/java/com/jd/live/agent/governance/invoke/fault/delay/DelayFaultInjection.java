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
package com.jd.live.agent.governance.invoke.fault.delay;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.invoke.fault.FaultInjection;
import com.jd.live.agent.governance.policy.service.fault.FaultInjectionPolicy;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A fault injection implementation that introduces delays in the request processing.
 *
 * @since 1.4.0
 */
@Extension("delay")
public class DelayFaultInjection implements FaultInjection {

    private static final Logger logger = LoggerFactory.getLogger(DelayFaultInjection.class);

    @Override
    public void acquire(FaultInjectionPolicy policy) {
        if (policy.getDelayTimeMs() != null && policy.getDelayTimeMs() > 0 &&
                (policy.getPercent() == null
                        || policy.getPercent() <= 0
                        || ThreadLocalRandom.current().nextInt(100) < policy.getPercent())) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Start delaying request by fault injection, delay time is " + policy.getDelayTimeMs() + "ms.");
                }
                Thread.sleep(policy.getDelayTimeMs());
            } catch (InterruptedException ignored) {
                // ignored
            }
        }
    }
}
