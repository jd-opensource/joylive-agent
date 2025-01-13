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
package com.jd.live.agent.plugin.transmission.pulsar.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.pulsar.v3.request.MetadataWriter;
import org.apache.pulsar.client.api.TypedMessageBuilder;

public class SendInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public SendInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        RequestContext.setAttribute(Carrier.ATTRIBUTE_MQ_PRODUCER, Boolean.TRUE);
        TypedMessageBuilder<?> builder = (TypedMessageBuilder<?>) ctx.getTarget();
        propagation.write(RequestContext.get(), MetadataWriter.of(builder));
    }

}
