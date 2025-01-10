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
package com.jd.live.agent.plugin.transmission.springweb.v5.request;

import com.jd.live.agent.governance.request.HeaderParser;
import org.springframework.http.HttpHeaders;

import java.util.Iterator;

public class HttpHeadersParser implements HeaderParser {

    private final HttpHeaders headers;

    public HttpHeadersParser(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public Iterator<String> getNames() {
        return headers.keySet().iterator();
    }

    @Override
    public Iterable<String> getHeaders(String key) {
        return headers.get(key);
    }

    @Override
    public String getHeader(String key) {
        return headers.getFirst(key);
    }

    @Override
    public boolean isDuplicable() {
        return true;
    }

    @Override
    public void addHeader(String key, String value) {
        headers.add(key, value);
    }

    @Override
    public void setHeader(String key, String value) {
        headers.set(key, value);
    }
}
