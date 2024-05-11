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
package com.jd.live.agent.plugin.router.dubbo.v2_6.request.invoke;

import com.jd.live.agent.governance.invoke.InboundInvocation.RpcInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.RpcOutboundInvocation;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.DubboRequest.DubboInboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.DubboRequest.DubboOutboundRequest;

/**
 * Represents a common contract for Dubbo invocations within an RPC system.
 * This interface is a marker for Dubbo-specific invocation classes, providing a shared
 * identity that can be used to group inbound and outbound Dubbo invocation types.
 */
public interface DubboInvocation {

    /**
     * Represents a Dubbo-specific inbound RPC invocation.
     * <p>
     * This class encapsulates the details of an inbound request received by a Dubbo service provider.
     * It extends the generic {@link RpcInboundInvocation} by specializing it for Dubbo's
     * communication protocols and request handling procedures.
     * </p>
     *
     * @see RpcInboundInvocation
     */
    class DubboInboundInvocation extends RpcInboundInvocation<DubboInboundRequest> implements DubboInvocation {

        /**
         * Constructs a new DubboInboundInvocation with the given request and context.
         *
         * @param request The {@link DubboInboundRequest} representing the incoming RPC request.
         * @param context The {@link InvocationContext} providing additional context about the invocation,
         *                such as metadata, invocation attributes, or other relevant information.
         */
        public DubboInboundInvocation(DubboInboundRequest request, InvocationContext context) {
            super(request, context);
        }
    }

    /**
     * Represents a Dubbo-specific outbound RPC invocation.
     * <p>
     * This class encapsulates the details of an outbound request to be sent by a Dubbo service consumer.
     * It extends the generic {@link RpcOutboundInvocation} by tailoring it to the requirements of Dubbo's
     * communication protocols and request dispatching procedures.
     * </p>
     *
     * @see RpcOutboundInvocation
     */
    class DubboOutboundInvocation extends RpcOutboundInvocation<DubboOutboundRequest> implements DubboInvocation {

        /**
         * Constructs a new DubboOutboundInvocation with the given request and context.
         *
         * @param request The {@link DubboOutboundRequest} representing the outgoing RPC request.
         * @param context The {@link InvocationContext} providing additional context about the invocation,
         *                such as metadata, invocation attributes, or other relevant information.
         */
        public DubboOutboundInvocation(DubboOutboundRequest request, InvocationContext context) {
            super(request, context);
        }
    }

}
