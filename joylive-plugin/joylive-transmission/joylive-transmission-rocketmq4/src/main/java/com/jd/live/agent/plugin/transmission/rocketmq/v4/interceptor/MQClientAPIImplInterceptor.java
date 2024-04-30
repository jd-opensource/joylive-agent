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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Cargo;
import org.apache.rocketmq.common.protocol.header.SendMessageRequestHeader;

public class MQClientAPIImplInterceptor extends InterceptorAdaptor {

    private static final char SPLITTER_KEY_VALUE = 1;

    private static final char SPLITTER_HEADER = 2;

    @Override
    public void onEnter(ExecutableContext ctx) {
        attachTag((SendMessageRequestHeader) ctx.getArguments()[3]);
    }

    private void attachTag(SendMessageRequestHeader header) {
        String properties = header.getProperties();
        StringBuilder builder = new StringBuilder();
        RequestContext.traverse(tag -> append(builder, tag));
        if (builder.length() > 0) {
            if (properties != null && !properties.isEmpty()) {
                builder.append(properties);
            } else {
                builder.deleteCharAt(builder.length() - 1);
            }
            header.setProperties(builder.toString());
        }
    }

    private void append(StringBuilder builder, Cargo cargo) {
        builder.append(cargo.getKey()).append(SPLITTER_KEY_VALUE).append(cargo.getValue()).append(SPLITTER_HEADER);
    }
}
