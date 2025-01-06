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
package com.jd.live.agent.plugin.transmission.rocketmq.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.bag.Propagation;
import org.apache.rocketmq.common.message.Message;

import java.util.Map;

import static com.jd.live.agent.governance.request.header.HeaderParser.StringHeaderParser.reader;
import static com.jd.live.agent.governance.request.header.HeaderParser.StringHeaderParser.writer;

public class MessageUtilInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public MessageUtilInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Message request = mc.getArgument(0);
        Message response = mc.getResult();
        Map<String, String> properties = request.getProperties();
        if (properties != null) {
            propagation.write(reader(request.getProperties()), writer(response.getProperties(), response::putUserProperty));
        }
    }
}
