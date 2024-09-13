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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.IOUtils;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import com.jd.live.agent.governance.response.ServiceError;
import feign.Response;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * FeignOutboundResponse
 *
 * @since 1.0.0
 */
public class FeignClusterResponse extends AbstractHttpOutboundResponse<Response> {

    private static final Logger logger = LoggerFactory.getLogger(FeignClusterResponse.class);

    private byte[] body;

    public FeignClusterResponse(Response response) {
        super(response);
        headers = new LazyObject<>(() -> parserHeader(response));
    }

    public FeignClusterResponse(ServiceError throwable, Predicate<Throwable> predicate) {
        super(throwable, predicate);
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

    @Override
    public Object getResult() {
        if (body == null) {
            Response.Body bodied = response == null ? null : response.body();
            if (bodied == null) {
                body = new byte[0];
            } else {
                try {
                    InputStream in = bodied.asInputStream();
                    body = IOUtils.read(in);
                    response = Response.builder()
                            .body(body)
                            .headers(response.headers())
                            .protocolVersion(response.protocolVersion())
                            .reason(response.reason())
                            .request(response.request())
                            .status(response.status())
                            .build();
                    Close.instance().close(in);
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                    body = new byte[0];
                }
            }
        }
        return body;
    }

}
