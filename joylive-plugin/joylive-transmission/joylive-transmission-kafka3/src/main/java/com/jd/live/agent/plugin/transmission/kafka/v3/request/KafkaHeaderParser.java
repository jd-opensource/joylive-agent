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
package com.jd.live.agent.plugin.transmission.kafka.v3.request;

import com.jd.live.agent.governance.request.HeaderFeature;
import com.jd.live.agent.governance.request.HeaderParser;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.CollectionUtils.*;

public class KafkaHeaderParser implements HeaderParser {

    private final Headers headers;

    public KafkaHeaderParser(Headers headers) {
        this.headers = headers;
    }

    @Override
    public Iterator<String> getNames() {
        return toIterator(headers.iterator(), this::getValue);
    }

    @Override
    public List<String> getHeaders(String key) {
        return toList(headers.headers(key), this::getValue);
    }

    @Override
    public String getHeader(String key) {
        Iterator<Header> iterator = headers.headers(key).iterator();
        Header header = !iterator.hasNext() ? null : iterator.next();
        return getValue(header);
    }

    @Override
    public int read(BiConsumer<String, Iterable<String>> consumer, Predicate<String> predicate) {
        return iterate(headers,
                header -> predicate == null || predicate.test(header.key()),
                header -> consumer.accept(header.key(), toList(getValue(header))));
    }

    @Override
    public HeaderFeature getFeature() {
        return HeaderFeature.DUPLICABLE;
    }

    @Override
    public void addHeader(String key, String value) {
        headers.add(key, value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void setHeader(String key, String value) {
        // TODO optimize
        Iterator<Header> iterator = headers.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().key().equals(key)) {
                iterator.remove();
                break;
            }
        }
        headers.add(key, value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8));
    }

    private String getValue(Header header) {
        byte[] value = header == null ? null : header.value();
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }
}
