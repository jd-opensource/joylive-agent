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
package com.jd.live.agent.plugin.protection.mongodb.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.interceptor.AbstractDbInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.mongodb.v4.request.MongodbRequest;
import com.mongodb.ServerAddress;
import com.mongodb.internal.binding.WriteBinding;

/**
 * ExecuteWriteCommandInterceptor
 */
public class ExecuteWriteCommandInterceptor extends AbstractDbInterceptor {

    public ExecuteWriteCommandInterceptor(PolicySupplier policySupplier) {
        super(policySupplier);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        WriteBinding writeBinding = ctx.getArgument(0);
        String database = ctx.getArgument(1);
        ServerAddress serverAddress = writeBinding
                .getWriteConnectionSource()
                .getServerDescription()
                .getAddress();
        protect((MethodContext) ctx, new MongodbRequest(serverAddress, database));
    }

}
