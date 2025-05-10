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
package com.jd.live.agent.plugin.protection.mariadb.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.interceptor.AbstractDbInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.mariadb.v3.request.LiveClientMessage;
import com.jd.live.agent.plugin.protection.mariadb.v3.request.MariadbRequest;
import org.mariadb.jdbc.client.impl.StandardClient;
import org.mariadb.jdbc.message.ClientMessage;

/**
 * SendQueryInterceptor
 */
public class SendQueryInterceptor extends AbstractDbInterceptor {

    public SendQueryInterceptor(PolicySupplier policySupplier) {
        super(policySupplier);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        StandardClient client = (StandardClient) ctx.getTarget();
        ClientMessage message = ctx.getArgument(0);
        if (!(message instanceof LiveClientMessage)) {
            protect(mc, new MariadbRequest(client, message));
        }
    }

}
