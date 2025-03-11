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
package com.jd.live.agent.governance.registry;

import com.jd.live.agent.governance.instance.Endpoint;

import java.util.Map;

/**
 * Unified service endpoint definition combining discovery and connectivity aspects.
 */
public interface ServiceEndpoint extends Endpoint {

    /**
     * Extended metadata for service instance. Metadata typically includes:
     *
     * @return Immutable key-value pairs (empty map permitted)
     */
    Map<String, String> getMetadata();
}
