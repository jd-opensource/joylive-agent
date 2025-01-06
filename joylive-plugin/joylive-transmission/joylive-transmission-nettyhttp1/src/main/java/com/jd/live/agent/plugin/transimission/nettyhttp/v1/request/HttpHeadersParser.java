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
package com.jd.live.agent.plugin.transimission.nettyhttp.v1.request;

import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.Iterator;
import java.util.List;

public class HttpHeadersParser implements HeaderReader, HeaderWriter {

    private final HttpHeaders headers;

    public HttpHeadersParser(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public Iterator<String> getNames() {
        return headers.names().iterator();
    }

    @Override
    public List<String> getHeaders(String key) {
        return headers.getAll(key);
    }

    @Override
    public String getHeader(String key) {
        return headers.get(key);
    }

    @Override
    public void setHeader(String key, String value) {
        headers.set(key, value);
    }
}
