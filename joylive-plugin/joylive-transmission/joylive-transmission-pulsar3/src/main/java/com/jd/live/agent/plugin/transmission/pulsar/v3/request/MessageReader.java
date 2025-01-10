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
package com.jd.live.agent.plugin.transmission.pulsar.v3.request;

import com.jd.live.agent.governance.request.HeaderReader;
import org.apache.pulsar.client.api.Message;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MessageReader implements HeaderReader {

    private final Message<?> message;

    public MessageReader(Message<?> message) {
        this.message = message;
    }

    @Override
    public Iterator<String> getNames() {
        return message.getProperties().keySet().iterator();
    }

    @Override
    public List<String> getHeaders(String key) {
        String value = message.getProperty(key);
        return value == null ? null : Collections.singletonList(value);
    }

    @Override
    public String getHeader(String key) {
        return message.getProperty(key);
    }
}
