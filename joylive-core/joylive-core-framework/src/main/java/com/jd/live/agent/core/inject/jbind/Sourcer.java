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
package com.jd.live.agent.core.inject.jbind;

/**
 * The {@code Sourcer} interface defines a contract for a component that is capable of providing
 * a source object based on a given context. This is useful in scenarios where an object needs to be
 * retrieved or instantiated as part of a larger process, such as dependency injection or event handling.
 *
 * @since 1.0.0
 */
public interface Sourcer {

    /**
     * Retrieves or constructs an object based on the provided context. The context object is expected
     * to contain any necessary state or information required to determine the correct object to return.
     *
     * @param context an object that provides the context for sourcing the object
     * @return the sourced object, which may be null if no object can be sourced from the given context
     */
    Object getSource(Object context);

}
