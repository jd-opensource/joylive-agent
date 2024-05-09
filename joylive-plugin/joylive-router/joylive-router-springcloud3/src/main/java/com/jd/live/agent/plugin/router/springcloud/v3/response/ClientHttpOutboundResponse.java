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

import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * ClientHttpOutboundResponse
 *
 * @since 1.0.0
 */
public class ClientHttpOutboundResponse extends AbstractHttpOutboundResponse<ClientHttpResponse> {

    public ClientHttpOutboundResponse(ClientHttpResponse response, Throwable throwable) {
        super(response, throwable);
    }

    @Override
    public String getCode() {
        try {
            return response == null ? "500" : String.valueOf(response.getStatusCode().value());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}