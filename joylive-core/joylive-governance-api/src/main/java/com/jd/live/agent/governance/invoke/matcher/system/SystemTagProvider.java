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
package com.jd.live.agent.governance.invoke.matcher.system;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.List;

/**
 * Provides system tags based on the request and key.
 */
@Extensible("SystemTagProvider")
public interface SystemTagProvider {

    /**
     * Retrieves values associated with the given key from the request.
     *
     * @param request The request object.
     * @return A list of values associated with the key.
     */
    List<String> getValues(ServiceRequest request);
}
