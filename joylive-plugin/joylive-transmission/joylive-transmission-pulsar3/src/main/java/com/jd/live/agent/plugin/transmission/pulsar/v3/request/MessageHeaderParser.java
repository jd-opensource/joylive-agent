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

import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.apache.pulsar.client.impl.TypedMessageBuilderImpl;
import org.apache.pulsar.common.api.proto.KeyValue;
import org.apache.pulsar.common.api.proto.MessageMetadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toIterator;

public class MessageHeaderParser implements HeaderReader, HeaderWriter {

    private final MessageMetadata metadata;

    public MessageHeaderParser(MessageMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public Iterator<String> getNames() {
        return toIterator(metadata.getPropertiesList().iterator(), KeyValue::getValue);
    }

    @Override
    public List<String> getHeaders(String key) {
        List<String> result = new ArrayList<>();
        for (KeyValue kv : metadata.getPropertiesList()) {
            if (kv.getKey().equals(key)) {
                result.add(kv.getValue());
            }
        }
        return result;
    }

    @Override
    public String getHeader(String key) {
        for (KeyValue kv : metadata.getPropertiesList()) {
            if (kv.getKey().equals(key)) {
                return kv.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean isDuplicable() {
        return false;
    }

    @Override
    public void addHeader(String key, String value) {
        metadata.addProperty().setValue(key).setValue(value);
    }

    @Override
    public void setHeader(String key, String value) {
        for (KeyValue kv : metadata.getPropertiesList()) {
            if (kv.getKey().equals(key)) {
                kv.setValue(value);
                return;
            }
        }
        metadata.addProperty().setValue(key).setValue(value);
    }

    public static MessageHeaderParser of(TypedMessageBuilder<?> builder) {
        if (builder instanceof TypedMessageBuilderImpl) {
            return new MessageHeaderParser(((TypedMessageBuilderImpl<?>) builder).getMetadataBuilder());
        } else {
            return null;
        }
    }
}
