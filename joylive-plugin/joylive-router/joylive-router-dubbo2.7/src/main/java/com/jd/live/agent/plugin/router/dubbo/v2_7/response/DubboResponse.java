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
package com.jd.live.agent.plugin.router.dubbo.v2_7.response;

import com.jd.live.agent.governance.response.AbstractRpcResponse.AbstractRpcOutboundResponse;
import com.jd.live.agent.governance.response.Response;
import org.apache.dubbo.rpc.Result;

import java.util.function.Predicate;

/**
 * Represents a generic response in the Dubbo RPC framework. This interface serves as a marker
 * for responses that are specific to Dubbo's communication model, allowing for a common handling
 * mechanism for all types of Dubbo responses.
 */
public interface DubboResponse {

    /**
     * A concrete implementation of a Dubbo response that extends {@code AbstractRpcOutboundResponse}
     * to provide specific handling for responses (or errors) in Dubbo RPC calls. This class encapsulates
     * the result of an RPC call, potentially including a result object, an exception, and a predicate
     * for response filtering or processing.
     */
    class DubboOutboundResponse extends AbstractRpcOutboundResponse<Result> implements DubboResponse {

        public DubboOutboundResponse(Result response) {
            super(response, null, null);
        }

        public DubboOutboundResponse(Throwable throwable, Predicate<Response> predicate) {
            super(null, throwable, predicate);
        }

        public DubboOutboundResponse(Result response, Throwable throwable, Predicate<Response> predicate) {
            super(response, throwable, predicate);
        }
    }
}
