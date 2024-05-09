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
package com.jd.live.agent.plugin.router.sofarpc.response;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.jd.live.agent.governance.response.AbstractRpcResponse.AbstractRpcOutboundResponse;
import com.jd.live.agent.governance.response.Response;

import java.util.function.Predicate;

/**
 * SofaRpcResponse
 *
 * @since 1.0.0
 */
public interface SofaRpcResponse {

    class SofaRpcOutboundResponse extends AbstractRpcOutboundResponse<SofaResponse> implements SofaRpcResponse {

        public SofaRpcOutboundResponse(SofaResponse response) {
            super(response, null, null);
        }

        public SofaRpcOutboundResponse(SofaRpcException throwable, Predicate<Response> predicate) {
            super(null, throwable, predicate);
        }
    }
}
