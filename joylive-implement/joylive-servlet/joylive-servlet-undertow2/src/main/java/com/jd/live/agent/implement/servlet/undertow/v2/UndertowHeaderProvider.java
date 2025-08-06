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
package com.jd.live.agent.implement.servlet.undertow.v2;

import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.core.util.map.MultiMap;
import com.jd.live.agent.governance.request.HeaderProvider;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.util.HeaderMap;


public class UndertowHeaderProvider implements HeaderProvider {

    private final HttpServletRequestImpl request;

    public UndertowHeaderProvider(HttpServletRequestImpl request) {
        this.request = request;
    }

    @Override
    public MultiMap<String, String> getHeaders() {
        HttpServerExchange exchange = request.getExchange();
        HeaderMap headers = exchange.getRequestHeaders();
        if (headers != null) {
            int count = headers.size();
            if (count == 0) {
                return null;
            }
            MultiMap<String, String> result = MultiLinkedMap.caseInsensitive(count);
            headers.getHeaderNames().forEach(header -> {
                result.setAll(header.toString(), headers.get(header));
            });
            return result;
        } else {
            return HttpUtils.parseHeader(request.getHeaderNames(), request::getHeaders);
        }
    }
}
