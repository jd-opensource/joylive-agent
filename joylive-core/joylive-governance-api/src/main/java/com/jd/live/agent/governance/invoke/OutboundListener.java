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

import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.response.ServiceResponse;

/**
 * Interface for handling service request callbacks.
 */
public interface OutboundListener {

    /**
     * Called when the service request completes successfully.
     *
     * @param endpoint the target endpoint
     * @param request  the service request that was made
     * @param response the response received from the service
     */
    void onSuccess(Endpoint endpoint, ServiceRequest request, ServiceResponse response);

    /**
     * Called when the service request fails.
     *
     * @param endpoint the target endpoint
     * @param request   the service request that was made
     * @param throwable the exception that caused the failure
     */
    void onFailure(Endpoint endpoint, ServiceRequest request, Throwable throwable);

}

