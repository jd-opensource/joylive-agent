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

import com.jd.live.agent.governance.request.HeaderFeature;
import com.jd.live.agent.governance.request.HeaderWriter;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

public class HttpMethodWriter implements HeaderWriter {

    private final HttpMethod method;

    public HttpMethodWriter(HttpMethod method) {
        this.method = method;
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
    public HeaderFeature getFeature() {
        return HeaderFeature.DUPLICABLE;
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
