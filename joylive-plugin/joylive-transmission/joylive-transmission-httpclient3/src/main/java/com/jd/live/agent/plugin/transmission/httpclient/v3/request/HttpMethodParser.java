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
package com.jd.live.agent.plugin.transmission.httpclient.v3.request;

import com.jd.live.agent.governance.request.HeaderParser;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.util.Iterator;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toIterator;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

public class HttpMethodParser implements HeaderParser {

    private final HttpMethod method;

    public HttpMethodParser(HttpMethod method) {
        this.method = method;
    }

    @Override
    public Iterator<String> getNames() {
        return toIterator(method.getRequestHeaders(), Header::getName);
    }

    @Override
    public List<String> getHeaders(String key) {
        return toList(method.getRequestHeaders(key), Header::getValue);
    }

    @Override
    public String getHeader(String key) {
        Header header = method.getRequestHeader(key);
        return header == null ? null : header.getValue();
    }

    @Override
    public boolean isDuplicable() {
        return true;
    }

    @Override
    public void addHeader(String key, String value) {
        method.addRequestHeader(key, value);
    }

    @Override
    public void setHeader(String key, String value) {
        method.setRequestHeader(key, value);
    }
}
