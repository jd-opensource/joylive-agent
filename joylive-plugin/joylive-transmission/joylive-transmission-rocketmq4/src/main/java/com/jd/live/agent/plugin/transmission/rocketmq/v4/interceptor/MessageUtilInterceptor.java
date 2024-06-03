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
package com.jd.live.agent.plugin.transmission.rocketmq.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;
import org.apache.rocketmq.common.message.Message;

import java.util.List;
import java.util.Map;

public class MessageUtilInterceptor extends InterceptorAdaptor {

    private final CargoRequire require;

    public MessageUtilInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        attachTag((Message) mc.getArgument(0), (Message) mc.getResult());
    }

    private void attachTag(Message request, Message response) {
        Map<String, String> properties = request.getProperties();
        if (properties != null) {
            properties.forEach((k, v) -> {
                if (require.match(k)) {
                    response.putUserProperty(k, v);
                }
            });
        }
    }
}
