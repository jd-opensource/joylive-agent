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

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * Factory interface for creating {@link HeaderProvider} instances to optimize
 * Servlet request header parsing in J2EE containers.
 */
@Extensible("HeaderProviderFactory")
public interface HeaderProviderFactory {

    /**
     * Creates a header provider instance for the given request object.
     *
     * @param request The underlying request object (e.g., HttpServletRequest)
     * @return Configured HeaderProvider instance
     */
    HeaderProvider create(Object request);

    /**
     * Returns the supported request type identifiers.
     */
    String[] getSupportTypes();

}

