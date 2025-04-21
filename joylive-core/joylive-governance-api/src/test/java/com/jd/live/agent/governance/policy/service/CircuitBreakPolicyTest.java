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
package com.jd.live.agent.governance.policy.service;

import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CircuitBreakPolicyTest {

    @Test
    void testProtectMode() {
        CircuitBreakPolicy policy = new CircuitBreakPolicy();
        policy.setOutlierMaxPercent(50);
        Assertions.assertTrue(policy.isProtectMode(1));
        Assertions.assertFalse(policy.isProtectMode(2));
        policy.setOutlierMaxPercent(100);
        Assertions.assertFalse(policy.isProtectMode(1));
        Assertions.assertFalse(policy.isProtectMode(2));
        // default is 50
        policy.setOutlierMaxPercent(0);
        Assertions.assertTrue(policy.isProtectMode(1));
        Assertions.assertFalse(policy.isProtectMode(2));
    }
}
