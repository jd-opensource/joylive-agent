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
package com.jd.live.agent.governance.invoke;

import com.jd.live.agent.governance.request.ServiceRequest;

/**
 * Interface for handling inbound service request callbacks.
 */
public interface InboundListener {

    /**
     * Called when the service request completes successfully.
     *
     * @param request the service request that was made
     */
    void onForward(ServiceRequest request);

    /**
     * Called when the service request fails.
     *
     * @param request   the service request that was made
     * @param throwable the exception that caused the failure
     */
    void onFailure(ServiceRequest request, Throwable throwable);

}

