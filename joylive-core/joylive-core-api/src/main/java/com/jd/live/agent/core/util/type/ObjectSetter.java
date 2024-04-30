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
package com.jd.live.agent.core.util.type;

/**
 * An interface that defines a method for setting a value on a target object.
 */
public interface ObjectSetter {

    /**
     * Sets a specified value on the given target object.
     *
     * @param target The object on which the value is to be set.
     * @param value  The value to set on the target object.
     */
    void set(Object target, Object value);
}

