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
package com.jd.live.agent.plugin.router.sofarpc.request;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;

/**
 * SofaRpcRequest
 *
 * @since 1.0.0
 */
public interface SofaRpcRequest {

    class SofaRpcInboundRequest extends AbstractRpcInboundRequest<SofaRequest> implements SofaRpcRequest {

        public SofaRpcInboundRequest(SofaRequest request) {
            super(request);
            this.service = request.getInterfaceName();
            String uniqueName = request.getTargetServiceUniqueName();
            int pos = uniqueName.lastIndexOf(':');
            this.group = pos < 0 ? null : uniqueName.substring(pos + 1);
            this.method = request.getMethodName();
            this.arguments = request.getMethodArgs();
            this.attachments = request.getRequestProps();
        }
    }

    class SofaRpcOutboundRequest extends AbstractRpcOutboundRequest<SofaRequest> implements SofaRpcRequest {

        public SofaRpcOutboundRequest(SofaRequest request) {
            super(request);
            this.service = request.getInterfaceName();
            String uniqueName = request.getTargetServiceUniqueName();
            int pos = uniqueName.lastIndexOf(':');
            this.group = pos < 0 ? null : uniqueName.substring(pos + 1);
            this.method = request.getMethodName();
            this.arguments = request.getMethodArgs();
            this.attachments = request.getRequestProps();
        }
    }
}
