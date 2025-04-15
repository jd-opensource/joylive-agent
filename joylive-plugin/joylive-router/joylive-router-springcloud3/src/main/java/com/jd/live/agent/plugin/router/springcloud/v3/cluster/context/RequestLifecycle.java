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
package com.jd.live.agent.plugin.router.springcloud.v3.cluster.context;

import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;

public interface RequestLifecycle {

    /**
     * A callback method executed before load-balancing.
     *
     * @param request the {@link Request} that will be used by the LoadBalancer to select
     *                a service instance
     */
    void onStart(Request<?> request);

    /**
     * A callback method executed after a service instance has been selected, before
     * executing the actual load-balanced request.
     *
     * @param request    the {@link Request} that has been used by the LoadBalancer to select
     *                   a service instance
     * @param lbResponse the {@link Response} returned by the LoadBalancer
     */
    void onStartRequest(Request<?> request, Response<?> lbResponse);

    /**
     * A callback method executed after load-balancing.
     *
     * @param completionContext the {@link CompletionContext} containing data relevant to
     *                          the load-balancing and the response returned from the selected service instance
     */
    void onComplete(CompletionContext<?, ?, ?> completionContext);

}
