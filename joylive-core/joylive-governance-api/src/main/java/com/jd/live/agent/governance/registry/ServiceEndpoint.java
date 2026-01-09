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

import java.net.URI;
import java.util.Map;

/**
 * Unified service endpoint definition combining discovery and connectivity aspects.
 */
public interface ServiceEndpoint extends Endpoint {

    @Override
    default String getScheme() {
        URI uri = getUri();
        return uri == null ? null : uri.getScheme();
    }

    /**
     * Retrieves the name of the service.
     *
     * @return the name of the service
     */
    String getService();

    /**
     * Indicates whether the service is secure (e.g., uses HTTPS or other secure protocols).
     * This method provides a default implementation that returns {@code false}.
     *
     * @return {@code true} if the service is secure, {@code false} otherwise
     */
    default boolean isSecure() {
        return false;
    }

    /**
     * Retrieves the URI of the service.
     * This method provides a default implementation that returns {@code null}.
     *
     * @return the URI of the service, or {@code null} if not available
     */
    default URI getUri() {
        return null;
    }

    /**
     * Extended metadata for service instance. Metadata typically includes:
     *
     * @return Immutable key-value pairs (empty map permitted)
     */
    Map<String, String> getMetadata();

    @Override
    default String getLabel(String key) {
        if (key == null) {
            return null;
        }
        Map<String, String> metadata = getMetadata();
        return metadata == null ? null : metadata.get(key);
    }
}
