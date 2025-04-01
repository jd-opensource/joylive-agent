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
package com.jd.live.agent.plugin.transmission.httpclient.v4.request;

import com.jd.live.agent.governance.request.HeaderWriter;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpRequestBase;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

public class HttpRequestBaseWriter implements HeaderWriter {

    private final HttpRequestBase requestBase;

    public HttpRequestBaseWriter(HttpRequestBase requestBase) {
        this.requestBase = requestBase;
    }

    @Override
    public List<String> getHeaders(String key) {
        return toList(requestBase.getHeaders(key), Header::getValue);
    }

    @Override
    public String getHeader(String key) {
        Header header = requestBase.getFirstHeader(key);
        return header == null ? null : header.getValue();
    }

    @Override
    public void addHeader(String key, String value) {
        requestBase.addHeader(key, value);
    }

    @Override
    public void setHeader(String key, String value) {
        requestBase.setHeader(key, value);
    }
}
