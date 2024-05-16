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
import feign.Response;

/**
 * FeignOutboundResponse
 *
 * @since 1.0.0
 */
public class FeignClusterResponse extends AbstractHttpOutboundResponse<Response> {

    public FeignClusterResponse(Response response) {
        super(response, null);
    }

    public FeignClusterResponse(Throwable throwable) {
        super(null, throwable);
    }

    public FeignClusterResponse(Response response, Throwable throwable) {
        super(response, throwable);
    }

    @Override
    public String getCode() {
        return response == null ? null : String.valueOf(response.status());
    }

}
