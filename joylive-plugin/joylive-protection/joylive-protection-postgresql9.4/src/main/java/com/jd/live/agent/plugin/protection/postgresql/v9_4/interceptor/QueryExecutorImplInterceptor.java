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
package com.jd.live.agent.plugin.protection.postgresql.v9_4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.governance.interceptor.AbstractDbInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.postgresql.v9_4.request.PostgresqlRequest;
import org.postgresql.core.ProtocolConnection;
import org.postgresql.core.Query;
import org.postgresql.core.v3.QueryExecutorImpl;

/**
 * QueryExecutorImplInterceptor
 */
public class QueryExecutorImplInterceptor extends AbstractDbInterceptor {

    public static final String FIELD_PROTO_CONNECTION = "protoConnection";

    private final FieldAccessor accessor;

    public QueryExecutorImplInterceptor(PolicySupplier policySupplier) {
        super(policySupplier);
        accessor = FieldAccessorFactory.getAccessor(QueryExecutorImpl.class, FIELD_PROTO_CONNECTION);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        QueryExecutorImpl executor = (QueryExecutorImpl) ctx.getTarget();
        Query query = ctx.getArgument(0);
        if (accessor != null) {
            ProtocolConnection connection = (ProtocolConnection) accessor.get(executor);
            protect(mc, new PostgresqlRequest(connection, query));
        }
    }

}
