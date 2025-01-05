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
package com.jd.live.agent.plugin.transmission.kafka.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.request.header.HeaderParser;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.jd.live.agent.governance.request.header.HeaderParser.MultiHeaderParser.reader;

public class KafkaConsumerRecordInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public KafkaConsumerRecordInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        restoreCargo((ConsumerRecord<?, ?>) ctx.getTarget());
    }

    private void restoreCargo(ConsumerRecord<?, ?> record) {
        String messageId = record.partition() + "-" + record.offset();
        String id = "Kafka3@" + record.topic() + "@" + messageId;
        propagation.read(
                RequestContext.getOrCreate(),
                reader(
                        new HeaderParser.WrappedMap<List<String>>() {
                            @Override
                            public Iterator<String> keyIterator() {
                                Headers headers = record.headers();
                                ArrayList<String> keys = new ArrayList<>();
                                headers.iterator().forEachRemaining(
                                        header -> keys.add(header.key())
                                );
                                return keys.iterator();
                            }

                            @Override
                            public List<String> get(String key) {
                                Iterable<Header> headers = record.headers().headers(key);
                                ArrayList<String> values = new ArrayList<>();
                                headers.iterator().forEachRemaining(
                                        header -> values.add(new String(header.value(), StandardCharsets.UTF_8))
                                );
                                return values;
                            }
                        },
                        () -> id
                )
        );
    }
}
