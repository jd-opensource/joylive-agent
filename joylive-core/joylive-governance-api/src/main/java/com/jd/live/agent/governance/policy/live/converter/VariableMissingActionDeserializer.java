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
package com.jd.live.agent.governance.policy.live.converter;

import com.jd.live.agent.core.parser.json.JsonConverter;
import com.jd.live.agent.governance.policy.live.VariableMissingAction;

public class VariableMissingActionDeserializer implements JsonConverter<Object, VariableMissingAction> {

    @Override
    public VariableMissingAction convert(Object source) {
        if (source instanceof String) {
            return VariableMissingAction.valueOf((String) source);
        } else if (source instanceof Number) {
            return (int) source == 2 ? VariableMissingAction.CENTER : VariableMissingAction.REJECT;
        }
        return null;
    }
}
