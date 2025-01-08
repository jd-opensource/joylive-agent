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
package com.jd.live.agent.plugin.transmission.grpc.request;

import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class MetadataParser implements HeaderReader, HeaderWriter {

    private static final Map<String, Key<String>> KEYS = new ConcurrentHashMap<>();

    private final Metadata metadata;

    public MetadataParser(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public Iterator<String> getNames() {
        return metadata.keys().iterator();
    }

    @Override
    public Iterable<String> getHeaders(String key) {
        return metadata.getAll(getOrCreate(key));
    }

    @Override
    public String getHeader(String key) {
        return metadata.get(getOrCreate(key));
    }

    @Override
    public boolean isDuplicable() {
        return true;
    }

    @Override
    public void addHeader(String key, String value) {
        metadata.put(getOrCreate(key), value);
    }

    @Override
    public void setHeader(String key, String value) {
        if (value == null) {
            return;
        }
        Key<String> metaKey = getOrCreate(key);
        // TODO optimize
        metadata.removeAll(metaKey);
        // always add
        metadata.put(metaKey, value);
    }

    private static Metadata.Key<String> getOrCreate(String key) {
        return KEYS.computeIfAbsent(key, k -> Metadata.Key.of(k, ASCII_STRING_MARSHALLER));
    }
}
