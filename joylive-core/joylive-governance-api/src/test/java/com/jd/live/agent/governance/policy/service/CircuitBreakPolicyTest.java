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

import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakLevel;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CircuitBreakPolicyTest {

    @Test
    void testProtectMode() {
        CircuitBreakPolicy policy = new CircuitBreakPolicy();
        policy.setOutlierMaxPercent(50);
        policy.setLevel(CircuitBreakLevel.SERVICE);
        Assertions.assertFalse(policy.isProtectMode(null, 1));
        Assertions.assertFalse(policy.isProtectMode(null, 2));
        policy.setLevel(CircuitBreakLevel.API);
        Assertions.assertFalse(policy.isProtectMode(null, 1));
        Assertions.assertFalse(policy.isProtectMode(null, 2));
        policy.setLevel(CircuitBreakLevel.INSTANCE);
        Assertions.assertTrue(policy.isProtectMode(null, 1));
        Assertions.assertFalse(policy.isProtectMode(null, 2));
        policy.setOutlierMaxPercent(100);
        Assertions.assertFalse(policy.isProtectMode(null, 1));
        Assertions.assertFalse(policy.isProtectMode(null, 2));
        // default is 50
        policy.setOutlierMaxPercent(0);
        Assertions.assertTrue(policy.isProtectMode(null, 1));
        Assertions.assertFalse(policy.isProtectMode(null, 2));
    }

    @Test
    void testProtected() {
        CircuitBreakPolicy policy = new CircuitBreakPolicy();
        policy.setLevel(CircuitBreakLevel.INSTANCE);

        // outlierMaxPercent=100%, no broken instances
        policy.setOutlierMaxPercent(100);
        Assertions.assertFalse(policy.isProtected(1));
        Assertions.assertFalse(policy.isProtected(2));

        // Simulate 1 broken instance (add inspector)
        policy.addInspector("instance-1", now -> null);

        // outlierMaxPercent=100%, 1 broken instance, 1 total → 1/1=100% at limit, we should start protecting!
        Assertions.assertTrue(policy.isProtected(1));
        Assertions.assertFalse(policy.isProtected(2));

        // Verify the bug scenario: isProtectMode (with +1) incorrectly returns true for new/unknown endpoints
        // isProtectMode: count=1+1=2, max=floor(1*100/100)=1, 2>1=true → wrong for acquireWhen
        Assertions.assertTrue(policy.isProtectMode(null, 1));

        // Wait, for instances already in inspectors, isProtectMode must return false to allow onError
        Assertions.assertFalse(policy.isProtectMode("instance-1", 1));

        // outlierMaxPercent=50%, 1 broken instance, 2 total → 1/2=50% at limit → NOT protected
        policy.setOutlierMaxPercent(50);
        Assertions.assertFalse(policy.isProtected(2));

        // outlierMaxPercent=50%, 1 broken instance, 1 total → 1/1=100% over 50% limit → protected
        Assertions.assertTrue(policy.isProtected(1));
    }
}
