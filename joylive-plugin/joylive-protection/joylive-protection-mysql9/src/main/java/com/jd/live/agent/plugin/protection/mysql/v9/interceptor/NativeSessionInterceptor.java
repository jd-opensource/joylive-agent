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
package com.jd.live.agent.plugin.protection.mysql.v9.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.interceptor.AbstractDbInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.mysql.v9.request.MysqlRequest;
import com.mysql.cj.PreparedQuery;
import com.mysql.cj.Query;
import com.mysql.cj.Session;
import com.mysql.cj.jdbc.JdbcPreparedStatement;

/**
 * NativeSessionInterceptor
 */
public class NativeSessionInterceptor extends AbstractDbInterceptor {

    public NativeSessionInterceptor(PolicySupplier policySupplier) {
        super(policySupplier);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Session session = (Session) ctx.getTarget();
        Query query = ctx.getArgument(0);
        String sql = ctx.getArgument(1);
        if (sql == null) {
            if (query instanceof PreparedQuery) {
                sql = ((PreparedQuery) query).getOriginalSql();
            } else if (query instanceof JdbcPreparedStatement) {
                sql = ((JdbcPreparedStatement) query).getPreparedSql();
            }
        }
        protect(mc, new MysqlRequest(session, sql));
    }

}
