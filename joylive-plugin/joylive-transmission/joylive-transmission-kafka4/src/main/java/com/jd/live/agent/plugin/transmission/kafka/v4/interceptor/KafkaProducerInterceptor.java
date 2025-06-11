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
package com.jd.live.agent.plugin.transmission.kafka.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.transmission.kafka.v4.request.KafkaHeaderParser;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaProducerInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public KafkaProducerInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ProducerRecord<?, ?> record = (ProducerRecord<?, ?>) ctx.getArguments()[0];
        Location location = context.isLiveEnabled() ? context.getLocation() : null;
        Propagation propagation = context.getPropagation();
        propagation.write(RequestContext.get(), location, new KafkaHeaderParser(record.headers()));
    }

}
