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

/**
 * ServiceResponse
 *
 * @since 1.0.0
 */
public interface ServiceResponse extends Response {

    /**
     * Defines an interface for outbound service response.
     * <p>
     * This interface represents the response received from another service or component from the current service。
     * </p>
     *
     * @since 1.0.0
     */
    interface OutboundResponse extends ServiceResponse {

    }
}
