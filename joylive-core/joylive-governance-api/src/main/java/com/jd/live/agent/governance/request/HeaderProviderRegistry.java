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
package com.jd.live.agent.governance.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of {@link HeaderProviderFactory} instances mapped by supported types.
 */
public class HeaderProviderRegistry {

    private final Map<String, HeaderProviderFactory> factories;

    public HeaderProviderRegistry(List<HeaderProviderFactory> factories) {
        this.factories = new HashMap<>(factories.size());
        factories.forEach(factory -> {
            for (String type : factory.getSupportTypes()) {
                this.factories.put(type, factory);
            }
        });
    }

    /**
     * Retrieves a factory for the given type.
     *
     * @param type The request type class to lookup
     * @return Associated factory, or null if no factory supports this type
     */
    public HeaderProviderFactory getFactory(Class<?> type) {
        return factories.get(type.getName());
    }

}
