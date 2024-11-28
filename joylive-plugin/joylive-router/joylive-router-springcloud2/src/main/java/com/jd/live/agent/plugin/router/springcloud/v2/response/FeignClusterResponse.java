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
package com.jd.live.agent.plugin.router.springcloud.v2.response;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.IOUtils;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.map.CaseInsensitiveLinkedMap;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.core.util.map.MultiMap;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import feign.Response;
import org.springframework.http.HttpHeaders;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

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
        this.headers = new UnsafeLazyObject<>(() -> parserHeader(response));
        this.cookies = new UnsafeLazyObject<>(() -> HttpUtils.parseCookie(response.headers().get(HttpHeaders.COOKIE)));
    }

    public FeignClusterResponse(ServiceError error, ErrorPredicate predicate) {
        super(error, predicate);
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
                    Response.Builder builder = Response.builder()
                            .body(body)
                            .headers(response.headers())
                            .reason(response.reason())
                            .request(response.request())
                            .status(response.status());
                    response = builder.build();
                    Close.instance().close(in);
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                    body = new byte[0];
                }
            }
        }
        return body;
    }

    private Map<String, List<String>> parserHeader(Response response) {
        if (response == null || response.headers() == null) {
            return null;
        }
        MultiMap<String, String> headers = new MultiLinkedMap<>(CaseInsensitiveLinkedMap::new);
        response.headers().forEach(headers::setAll);
        return headers;
    }

}
