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
package com.jd.live.agent.plugin.protection.jdbc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.jdbc.sql.LiveConnection;

import java.sql.Connection;
import java.util.function.Consumer;

import static com.jd.live.agent.governance.util.DatabaseUtils.ADDRESS;

/**
 * DataSourceInterceptor
 */
public class DataSourceInterceptor extends AbstractDbConnectionInterceptor<Connection, LiveConnection> {

    public DataSourceInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher) {
        super(policySupplier, publisher);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Connection oldConnection = mc.getResult();
        String address = ADDRESS.get();
        if (address != null) {
            mc.setResult(createConnection(address, oldConnection));
        }
    }

    @Override
    protected LiveConnection doCreateConnection(String address, Connection connection, Consumer<LiveConnection> close) {
        return new LiveConnection(address, connection, close);
    }
}
