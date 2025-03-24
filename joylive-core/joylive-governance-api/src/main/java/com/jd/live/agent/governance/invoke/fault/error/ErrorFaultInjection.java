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
package com.jd.live.agent.governance.invoke.fault.error;

import com.jd.live.agent.bootstrap.exception.FaultException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.invoke.fault.FaultInjection;
import com.jd.live.agent.governance.policy.service.fault.FaultInjectionPolicy;

import java.util.Random;

/**
 * A fault injection implementation that introduces errors in the request processing.
 *
 * @since 1.4.0
 */
@Extension("error")
public class ErrorFaultInjection implements FaultInjection {

    @Override
    public void acquire(FaultInjectionPolicy policy, Random random) {
        if (policy.getPercent() <= 0 || random.nextInt(100) < policy.getPercent()) {
            String errorMsg = policy.getErrorMsg() == null ? "Error by fault injection" : policy.getErrorMsg();
            throw new FaultException(policy.getErrorCode(), errorMsg);
        }
    }
}
