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
package com.jd.live.agent.implement.variable.bkdr;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.policy.variable.UnitFunction;

@Extension(value = "BKDRHash")
public class BKDRHash implements UnitFunction {

    @Override
    public int compute(String variable, int modulo) {
        if (variable != null && modulo != 0) {
            return Math.abs(variable.toLowerCase().hashCode() % modulo);
        } else {
            return 0;
        }
    }
}
