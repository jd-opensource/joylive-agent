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
import com.jd.live.agent.governance.response.ServiceResponse;

/**
 * Interface for handling outbound service request callbacks.
 */
public interface OutboundListener {

    /**
     * Called when an outbound invocation is forwarded. This is a default method and returns true by default.
     *
     * @param endpoint   the endpoint to which the invocation is forwarded.
     * @param invocation the outbound invocation that was forwarded.
     * @return true if the forwarding was successful, false otherwise.
     */
    default boolean onForward(Endpoint endpoint, OutboundInvocation<?> invocation) {
        return true;
    }

    /**
     * Called when an endpoint is canceled. This is a default method and does nothing by default.
     *
     * @param endpoint   the endpoint to which the invocation was sent.
     * @param invocation the outbound invocation that was canceled.
     */
    default void onCancel(Endpoint endpoint, OutboundInvocation<?> invocation) {
    }

    /**
     * Called when an outbound invocation completes successfully.
     *
     * @param endpoint   the endpoint to which the invocation was sent.
     * @param invocation the outbound invocation that was successful.
     * @param response   the response received from the service.
     */
    void onSuccess(Endpoint endpoint, OutboundInvocation<?> invocation, ServiceResponse response);

    /**
     * Called when an outbound invocation fails.
     *
     * @param endpoint   the endpoint to which the invocation was sent.
     * @param invocation the outbound invocation that failed.
     * @param throwable  the exception that caused the failure.
     */
    void onFailure(Endpoint endpoint, OutboundInvocation<?> invocation, Throwable throwable);
}

