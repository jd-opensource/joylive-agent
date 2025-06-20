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
package com.jd.live.agent.plugin.failover.druid.interceptor;

import com.alibaba.druid.pool.DruidAbstractDataSource;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.db.DbUrlParser;
import com.jd.live.agent.governance.db.jdbc.connection.DriverConnection;
import com.jd.live.agent.governance.db.jdbc.datasource.LiveDataSource;
import com.jd.live.agent.governance.interceptor.AbstractJdbcConnectionInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.failover.druid.connection.DruidPooledConnection;
import com.jd.live.agent.plugin.failover.druid.datasource.DruidLiveDataSource;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * DruidCreateConnectionInterceptor
 */
public class DruidJdbcConnectionInterceptor extends AbstractJdbcConnectionInterceptor<DruidPooledConnection> {

    public DruidJdbcConnectionInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    protected DataSource getDataSource(ExecutableContext ctx) {
        return (DruidAbstractDataSource) ctx.getTarget();
    }

    @Override
    protected LiveDataSource build(DataSource dataSource) {
        DruidAbstractDataSource druid = ((DruidAbstractDataSource) dataSource);
        return new DruidLiveDataSource(druid, DbUrlParser.parse(druid.getUrl(), parsers::get));
    }

    @Override
    protected DruidPooledConnection build(Connection connection, DriverConnection driver, MethodContext ctx) {
        return new DruidPooledConnection(connection, driver, closer);
    }
}
