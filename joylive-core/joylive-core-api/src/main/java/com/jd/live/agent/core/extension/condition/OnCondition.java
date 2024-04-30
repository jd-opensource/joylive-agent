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
package com.jd.live.agent.core.extension.condition;

/**
 * An abstract base class that implements the {@link Condition} interface, providing a common foundation for
 * condition implementations. This class can be extended by concrete condition implementations to simplify the
 * process of checking conditions.
 */
public abstract class OnCondition implements Condition {

    protected boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
