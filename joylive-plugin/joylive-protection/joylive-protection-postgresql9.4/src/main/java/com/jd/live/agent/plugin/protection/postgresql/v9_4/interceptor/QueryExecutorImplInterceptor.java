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
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.interceptor.AbstractDbInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.postgresql.v9_4.request.PostgresqlRequest;
import org.postgresql.core.ProtocolConnection;
import org.postgresql.core.Query;
import org.postgresql.core.v3.QueryExecutorImpl;

/**
 * SendQueryInterceptor
 */
public class QueryExecutorImplInterceptor extends AbstractDbInterceptor {

    public static final String FIELD_PROTO_CONNECTION = "protoConnection";
    private final FieldDesc fieldDesc;

    public QueryExecutorImplInterceptor(PolicySupplier policySupplier) {
        super(policySupplier);
        fieldDesc = ClassUtils.describe(QueryExecutorImpl.class).getFieldList().getField(FIELD_PROTO_CONNECTION);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        QueryExecutorImpl executor = (QueryExecutorImpl) ctx.getTarget();
        if (fieldDesc != null) {
            ProtocolConnection connection = (ProtocolConnection) fieldDesc.get(executor);
            protect((MethodContext) ctx, new PostgresqlRequest(connection, (Query) ctx.getArguments()[0]));
        }
    }

}
