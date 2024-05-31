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
package com.jd.live.agent.plugin.router.springcloud.v3.response;

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * BlockingRouteResponse
 *
 * @since 1.0.0
 */
public class BlockingClusterResponse extends AbstractHttpOutboundResponse<ClientHttpResponse> {

    public BlockingClusterResponse(ClientHttpResponse response) {
        super(response, null);
        this.headers = new LazyObject<>(response.getHeaders());
    }

    public BlockingClusterResponse(Throwable throwable) {
        super(null, throwable);
    }

    public BlockingClusterResponse(ClientHttpResponse response, Throwable throwable) {
        super(response, throwable);
    }

    @Override
    public String getCode() {
        try {
            HttpStatus status = response == null ? null : response.getStatusCode();
            return status == null ? null : String.valueOf(status.value());
        } catch (IOException e) {
            return String.valueOf(INTERNAL_SERVER_ERROR.value());
        }
    }

}
