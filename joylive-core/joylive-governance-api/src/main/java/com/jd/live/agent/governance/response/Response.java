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
package com.jd.live.agent.governance.response;

import com.jd.live.agent.bootstrap.util.Attributes;

/**
 * Defines the structure for a response object in a system that processes operations
 * which can result in success or failure. The {@code Response} interface extends
 * {@code Attributes} to include additional details specific to the response such
 * as status codes, exceptions, and the original response data.
 *
 * @since 1.0.0
 */
public interface Response extends Attributes {

    /**
     * Retrieves the original response object. This method provides access to the
     * complete response data as received from the operation.
     *
     * @return An {@code Object} representing the original response.
     */
    default Object getResponse() {
        return null;
    }
}
