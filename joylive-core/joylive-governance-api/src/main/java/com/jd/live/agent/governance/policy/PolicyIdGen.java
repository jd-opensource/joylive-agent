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
package com.jd.live.agent.governance.policy;

import com.jd.live.agent.core.util.URI;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@code PolicyIdGen} interface defines a contract for generating unique identifiers that can be
 * used to supplement information with an ID. It provides a method for generating an ID and associating
 * it with a given URL and set of tags.
 *
 * @see Supplier
 * @see Map
 */
public interface PolicyIdGen {

    /**
     * Generates a unique identifier with the provided URI
     *
     * @param uriSupplier a {@code Supplier<URI>} that provides the URI.
     */
    void supplement(Supplier<URI> uriSupplier);

}