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
package com.jd.live.agent.plugin.router.gprc.request.invoke;

import com.jd.live.agent.governance.invoke.InboundInvocation.RpcInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.RpcOutboundInvocation;
import com.jd.live.agent.plugin.router.gprc.request.GRpcRequest.GRpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.request.GRpcRequest.GRpcInboundRequest;

public interface GRpcInvocation {

    class GRpcInboundInvocation extends RpcInboundInvocation<GRpcInboundRequest> implements GRpcInvocation {

        public GRpcInboundInvocation(GRpcInboundRequest request, InvocationContext context) {
            super(request, context);
        }
    }

    class GRpcOutboundInvocation extends RpcOutboundInvocation<GRpcOutboundRequest> implements GRpcInvocation {

        public GRpcOutboundInvocation(GRpcOutboundRequest request, InvocationContext context) {
            super(request, context);
        }
    }
}
