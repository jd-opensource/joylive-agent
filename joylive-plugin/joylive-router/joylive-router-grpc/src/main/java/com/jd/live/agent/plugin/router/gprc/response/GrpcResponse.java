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
package com.jd.live.agent.plugin.router.gprc.response;

import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.response.AbstractRpcResponse.AbstractRpcOutboundResponse;
import io.grpc.Status;

/**
 * Represents a generic response in the Dubbo RPC framework. This interface serves as a marker
 * for responses that are specific to Dubbo's communication model, allowing for a common handling
 * mechanism for all types of Dubbo responses.
 */
public interface GrpcResponse {

    class GrpcOutboundResponse extends AbstractRpcOutboundResponse<Object> implements GrpcResponse {

        private final Status status;

        public GrpcOutboundResponse(Object response) {
            super(response, null);
            status = Status.OK;
        }

        public GrpcOutboundResponse(ServiceError error, ErrorPredicate retryPredicate) {
            this(error, retryPredicate, null);
        }

        public GrpcOutboundResponse(ServiceError error, ErrorPredicate retryPredicate, Status status) {
            super(null, error, retryPredicate);
            this.status = status != null ? status : getStatus(error.getThrowable());
        }

        @Override
        public String getCode() {
            return String.valueOf(status.getCode().value());
        }

        private Status getStatus(Throwable throwable) {
            return throwable == null ? Status.INTERNAL : Status.fromThrowable(throwable);
        }
    }
}
