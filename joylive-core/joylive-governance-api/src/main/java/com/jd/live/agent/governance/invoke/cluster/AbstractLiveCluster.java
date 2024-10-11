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
package com.jd.live.agent.governance.invoke.cluster;

import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

/**
 * An abstract implementation of the LiveCluster interface.
 *
 * @param <R> The type of outbound request.
 * @param <O> The type of outbound response.
 * @param <E> The type of endpoint.
 * @param <T> The type of throwable.
 */
public abstract class AbstractLiveCluster<R extends OutboundRequest,
        O extends OutboundResponse,
        E extends Endpoint,
        T extends Throwable> implements LiveCluster<R, O, E, T> {

}
