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
package com.jd.live.agent.plugin.protection.kafka.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.auth.Permission;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.InvalidRequestException;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getAccessor;

public class DoSendInterceptor extends AbstractMessageInterceptor {

    public DoSendInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ProducerRecord<?, ?> record = ctx.getArgument(0);
        ProducerConfig config = Accessors.producerConfig.get(ctx.getTarget(), ProducerConfig.class);
        String address = config == null ? null : config.getString(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        Permission permission = isProduceReady(record.topic(), address);
        if (!permission.isSuccess()) {
            mc.setResult(Futures.future(new InvalidRequestException(permission.getMessage())));
        }
    }

    private static class Accessors {
        private static final UnsafeFieldAccessor producerConfig = getAccessor(KafkaProducer.class, "producerConfig");
    }
}
