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
package com.jd.live.agent.plugin.router.springgateway.v4.response;

import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import com.jd.live.agent.governance.response.ServiceError;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.function.Predicate;

/**
 * GatewayClusterResponse
 *
 * @since 1.0.0
 */
public class GatewayClusterResponse extends AbstractHttpOutboundResponse<ServerHttpResponse> {

    private String body;

    public GatewayClusterResponse(ServerHttpResponse response) {
        super(response);
    }

    public GatewayClusterResponse(ServiceError throwable, Predicate<Throwable> predicate) {
        super(throwable, predicate);
    }

    @Override
    public String getCode() {
        HttpStatusCode status = response == null ? null : response.getStatusCode();
        return status == null ? null : String.valueOf(status.value());
    }

}
