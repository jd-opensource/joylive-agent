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

/**
 * Interface for handling inbound invocation callbacks.
 */
public interface InboundListener {

    /**
     * Called when an inbound invocation is forwarded.
     *
     * @param invocation the inbound invocation that was forwarded.
     */
    void onForward(InboundInvocation<?> invocation);

    /**
     * Called when an inbound invocation fails.
     *
     * @param invocation the inbound invocation that failed.
     * @param throwable  the exception that caused the failure.
     */
    void onFailure(InboundInvocation<?> invocation, Throwable throwable);
}

