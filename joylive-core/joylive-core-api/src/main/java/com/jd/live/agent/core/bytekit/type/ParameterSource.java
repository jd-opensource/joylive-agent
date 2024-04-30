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

import java.util.List;

/**
 * The {@code ParameterSource} interface defines a contract for objects that can provide
 * access to a list of parameter descriptions. This interface is typically used by components
 * that need to introspect or manipulate parameters of a method or a constructor, for example.
 *
 * @since 1.0.0
 */
public interface ParameterSource {
    /**
     * Retrieves a list of parameter descriptions associated with this source.
     * Each parameter description provides detailed information about a single parameter,
     * which can include the type, name, annotations, and other metadata.
     *
     * @return a List of {@code ParameterDesc} objects representing the parameters
     */
    List<ParameterDesc> getParameters();
}
