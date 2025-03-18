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
package com.jd.live.agent.plugin.router.springgateway.v2_1.request;

import com.jd.live.agent.governance.request.HeaderFeature;
import com.jd.live.agent.governance.request.HeaderParser;
import org.springframework.http.HttpHeaders;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.CollectionUtils.iterate;

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
    public int read(BiConsumer<String, Iterable<String>> consumer, Predicate<String> predicate) {
        return consumer == null ? 0 : iterate(headers, predicate, consumer::accept);
    }

    @Override
    public HeaderFeature getFeature() {
        return HeaderFeature.DUPLICABLE;
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
