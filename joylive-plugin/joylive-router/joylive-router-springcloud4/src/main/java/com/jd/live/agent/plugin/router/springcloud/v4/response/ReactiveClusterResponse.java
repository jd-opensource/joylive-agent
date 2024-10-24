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
package com.jd.live.agent.plugin.router.springcloud.v4.response;

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * ReactiveRouteResponse
 *
 * @since 1.0.0
 */
public class ReactiveClusterResponse extends AbstractHttpOutboundResponse<ClientResponse> {

    private String body;

    public ReactiveClusterResponse(ClientResponse response) {
        super(response);
        this.headers = new UnsafeLazyObject<>(() -> response.headers().asHttpHeaders());
        this.cookies = new UnsafeLazyObject<>(() -> HttpUtils.parseCookie(response.cookies(), ResponseCookie::getValue));
    }

    public ReactiveClusterResponse(ServiceError error, ErrorPredicate predicate) {
        super(error, predicate);
    }

    @Override
    public String getCode() {
        HttpStatusCode status = response == null ? null : response.statusCode();
        return status == null ? null : String.valueOf(status.value());
    }

    @Override
    public Object getResult() {
        if (body == null) {
            if (response == null) {
                body = "";
            } else {
                body = response.bodyToMono(String.class).block();
                response = response.mutate().body(body).build();
            }
        }
        return body;
    }

}
