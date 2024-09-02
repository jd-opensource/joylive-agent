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

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import feign.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FeignOutboundResponse
 *
 * @since 1.0.0
 */
public class FeignClusterResponse extends AbstractHttpOutboundResponse<Response> {

    public FeignClusterResponse(Response response) {
        this(response, null);
    }

    public FeignClusterResponse(Throwable throwable) {
        this(null, throwable);
    }

    public FeignClusterResponse(Response response, Throwable throwable) {
        super(response, throwable);
        headers = new LazyObject<>(() -> parserHeader(response));
    }

    private Map<String, List<String>> parserHeader(Response response) {
        if (response == null || response.headers() == null) {
            return null;
        }
        Map<String, List<String>> headers = new HashMap<>();
        response.headers().forEach((k, v) -> {
            if (v instanceof List) {
                headers.put(k, (List<String>) v);
            } else {
                headers.put(k, new ArrayList<>(v));
            }
        });
        return headers;
    }

    @Override
    public String getCode() {
        return response == null ? null : String.valueOf(response.status());
    }

}
