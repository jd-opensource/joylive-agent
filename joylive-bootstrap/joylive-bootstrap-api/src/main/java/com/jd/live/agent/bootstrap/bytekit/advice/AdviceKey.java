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
package com.jd.live.agent.bootstrap.bytekit.advice;

import lombok.Getter;

/**
 * Represents a unique key for advice based on its description and the class loader used to load the advised class.
 */
@Getter
public class AdviceKey {

    private final String description;

    private final ClassLoader classLoader;

    private final int hashCode;

    public AdviceKey(String description, ClassLoader classLoader) {
        this.description = description;
        this.classLoader = classLoader;
        this.hashCode = description.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AdviceKey)) {
            return false;
        }
        AdviceKey adviceKey = (AdviceKey) o;
        return classLoader == adviceKey.classLoader && description.equals(adviceKey.description);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
