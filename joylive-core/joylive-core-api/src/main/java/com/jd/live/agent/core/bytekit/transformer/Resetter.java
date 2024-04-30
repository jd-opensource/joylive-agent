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
package com.jd.live.agent.core.bytekit.transformer;

/**
 * The {@code Resetter} interface defines a method for resetting the state of an object.
 * Implementing this interface allows an object to be restored to its initial state,
 * typically for the purpose of reusability or to clear any internal state that may have
 * been modified during use.
 *
 * @since 1.0.0
 */
public interface Resetter {

    /**
     * Resets the state of the implementing object to its initial or default state.
     * This method should ensure that the object is in a state as if it was newly created,
     * with all internal state cleared or set to default values.
     */
    void reset();

}
