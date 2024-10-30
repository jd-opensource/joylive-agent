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
package com.jd.live.agent.plugin.router.gprc.request;

import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.cluster.GrpcCluster;

public interface GrpcRequest {

    class GrpcInboundRequest<T> extends AbstractRpcInboundRequest<T> implements GrpcRequest {

        public GrpcInboundRequest(T request) {
            super(request);
        }

        @Override
        public String getClientIp() {
           return null;
        }

        @Override
        public boolean isSystem() {
            return false;
        }
    }

    class GrpcOutboundRequest<T> extends AbstractRpcOutboundRequest<T> implements GrpcRequest {

        public GrpcOutboundRequest(T request) {
            super(request);

        }

        public GrpcOutboundRequest(T request, GrpcCluster cluster) {
            super(request);

        }

        @Override
        public boolean isSystem() {
            return false;
        }
    }
}
