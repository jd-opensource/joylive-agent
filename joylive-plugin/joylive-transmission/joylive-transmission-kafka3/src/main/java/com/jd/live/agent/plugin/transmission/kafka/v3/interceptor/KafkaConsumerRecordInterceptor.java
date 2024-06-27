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
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class KafkaConsumerRecordInterceptor extends InterceptorAdaptor {

    private final CargoRequire require;

    public KafkaConsumerRecordInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        restoreCargo((ConsumerRecord<?, ?>) ctx.getTarget());
    }

    private void restoreCargo(ConsumerRecord<?, ?> record) {
        String messageId = record.partition() + "-" + record.offset();
        String id = "Kafka3@" + record.topic() + "@" + messageId;
        RequestContext.restore(() -> id,
                carrier -> carrier.addCargo(require, record.headers(), Header::key, this::getValue));
    }

    private String getValue(Header header) {
        byte[] value = header.value();
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }
}
