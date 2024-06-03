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
package com.jd.live.agent.plugin.transmission.kafka.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;

public class KafkaProducerInterceptor extends InterceptorAdaptor {

    private static final int ARGUMENT_MESSAGE_INDEX = 0;

    @Override
    public void onEnter(ExecutableContext ctx) {
        attachCargo((ProducerRecord<?, ?>) ctx.getArguments()[0]);
    }

    private void attachCargo(ProducerRecord<?, ?> record) {
        Headers headers = record.headers();
        RequestContext.cargos((k, v) -> headers.add(k, v == null ? null : k.getBytes(StandardCharsets.UTF_8)));
    }
}
