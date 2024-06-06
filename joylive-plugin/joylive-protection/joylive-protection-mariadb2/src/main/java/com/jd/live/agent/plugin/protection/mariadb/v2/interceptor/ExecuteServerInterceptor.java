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
package com.jd.live.agent.plugin.protection.mariadb.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.interceptor.AbstractDbInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.mariadb.v2.request.MariadbRequest;
import org.mariadb.jdbc.internal.com.read.dao.Results;
import org.mariadb.jdbc.internal.com.send.parameters.ParameterHolder;
import org.mariadb.jdbc.internal.protocol.AbstractQueryProtocol;
import org.mariadb.jdbc.internal.protocol.Protocol;
import org.mariadb.jdbc.internal.util.dao.ServerPrepareResult;

import java.util.List;

/**
 * ExecuteServerInterceptor
 */
public class ExecuteServerInterceptor extends AbstractDbInterceptor {

    public ExecuteServerInterceptor(PolicySupplier policySupplier) {
        super(policySupplier);
    }

    /**
     * Enhanced logic before method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see AbstractQueryProtocol#executeBatchServer(boolean, ServerPrepareResult, Results, String, List, boolean)
     * @see AbstractQueryProtocol#executePreparedQuery(boolean, ServerPrepareResult, Results, ParameterHolder[])
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        Protocol protocol = (Protocol) ctx.getTarget();
        String sql = ((ServerPrepareResult) ctx.getArguments()[1]).getSql();
        protect((MethodContext) ctx, new MariadbRequest(protocol, sql));
    }

}
