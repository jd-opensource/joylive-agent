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
package com.jd.live.agent.plugin.transmission.pulsar.request;

import com.jd.live.agent.governance.request.HeaderReader;
import org.apache.pulsar.client.api.Message;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.CollectionUtils.iterate;
import static com.jd.live.agent.core.util.CollectionUtils.singletonList;

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
        // read only
        return singletonList(message.getProperty(key));
    }

    @Override
    public String getHeader(String key) {
        return message.getProperty(key);
    }

    @Override
    public int read(BiConsumer<String, Iterable<String>> consumer, Predicate<String> predicate) {
        // read only
        return iterate(message.getProperties(), predicate, (key, value) -> consumer.accept(key, singletonList(value)));
    }
}
