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
package com.jd.live.agent.plugin.transmission.rocketmq.v5.request;

import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;
import org.apache.rocketmq.common.message.Message;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class MessageParser implements HeaderWriter, HeaderReader {

    private final Message message;

    public MessageParser(Message message) {
        this.message = message;
    }

    @Override
    public Iterator<String> getNames() {
        Map<String, String> props = message.getProperties();
        return props == null ? Collections.emptyIterator() : props.keySet().iterator();
    }

    @Override
    public Iterable<String> getHeaders(String key) {
        String property = message.getProperty(key);
        return property == null ? null : Collections.singletonList(property);
    }

    @Override
    public String getHeader(String key) {
        return message.getProperty(key);
    }

    @Override
    public boolean isDuplicable() {
        return false;
    }

    @Override
    public void addHeader(String key, String value) {
        message.putUserProperty(key, value);
    }

    @Override
    public void setHeader(String key, String value) {
        message.putUserProperty(key, value);
    }
}
