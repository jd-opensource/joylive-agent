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
import org.mariadb.jdbc.internal.util.dao.ClientPrepareResult;

import java.nio.charset.Charset;

/**
 * ExecuteQueryInterceptor
 */
public class ExecuteQueryInterceptor extends AbstractDbInterceptor {

    public ExecuteQueryInterceptor(PolicySupplier policySupplier) {
        super(policySupplier);
    }

    /**
     * Enhanced logic before method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see AbstractQueryProtocol#executeQuery(boolean, Results, String, Charset)
     * @see AbstractQueryProtocol#executeQuery(boolean, Results, ClientPrepareResult, ParameterHolder[])
     * @see AbstractQueryProtocol#executeQuery(boolean, Results, ClientPrepareResult, ParameterHolder[], int)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object argument = ctx.getArguments()[2];
        String sql = null;
        if (argument instanceof ClientPrepareResult) {
            sql = ((ClientPrepareResult) argument).getSql();
        } else {
            sql = (String) argument;
        }
        protect((MethodContext) ctx, new MariadbRequest((Protocol) ctx.getTarget(), sql));
    }

}
