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
package com.jd.live.agent.plugin.transmission.rabbitmq.v5.request;

import com.jd.live.agent.governance.request.HeaderParser;
import com.rabbitmq.client.BasicProperties;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class EnvelopeParser implements HeaderParser {

    private final BasicProperties props;

    public EnvelopeParser(BasicProperties props) {
        this.props = props;
    }

    @Override
    public Iterator<String> getNames() {
        Map<String, Object> headers = props.getHeaders();
        return headers == null ? null : headers.keySet().iterator();
    }

    @Override
    public Iterable<String> getHeaders(String key) {
        Map<String, Object> headers = props.getHeaders();
        Object value = headers == null ? null : headers.get(key);
        return value == null ? null : Collections.singletonList(value.toString());
    }

    @Override
    public String getHeader(String key) {
        Map<String, Object> headers = props.getHeaders();
        Object value = headers == null ? null : headers.get(key);
        return value == null ? null : value.toString();
    }

    @Override
    public void addHeader(String key, String value) {
        Map<String, Object> headers = props.getHeaders();
        if (headers != null) {
            headers.put(key, value);
        }
    }

    @Override
    public void setHeader(String key, String value) {
        Map<String, Object> headers = props.getHeaders();
        if (headers != null) {
            headers.put(key, value);
        }
    }
}
