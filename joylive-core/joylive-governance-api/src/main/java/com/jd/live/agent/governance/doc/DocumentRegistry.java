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
package com.jd.live.agent.governance.doc;

import java.util.List;

/**
 * Central registry for API service documentation.
 *
 * <p>Tracks ServiceDoc instances and provides discovery capability.</p>
 */
public interface DocumentRegistry {

    String COMPONENT_SERVICE_DOC_REGISTRY = "ServiceDocRegistry";

    /**
     * Adds service documentation to the registry.
     *
     * @param doc service documentation to register (non-null)
     */
    void register(Document doc);

    /**
     * Removes service documentation from the registry.
     *
     * @param doc service documentation to unregister (non-null)
     */
    void unregister(Document doc);

    /**
     * Gets all registered service documentation.
     *
     * @return immutable list of documentation (non-null, possibly empty)
     */
    List<Document> getDocuments();
}