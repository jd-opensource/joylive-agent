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
package com.jd.live.agent.core.bytekit.type;

/**
 * The {@code NamedElement} interface represents an element that can be identified by a name.
 * It provides a method to retrieve the actual name of the element, which can be useful for
 * various purposes such as display, comparison, or identification within a collection of elements.
 *
 * @since 1.0.0
 */
public interface NamedElement {
    /**
     * Returns the actual name of the element. The name is expected to be a non-null and
     * non-empty string that uniquely identifies the element within its context.
     *
     * @return a String representing the name of the element
     */
    String getActualName();
}
