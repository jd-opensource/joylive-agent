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

import com.alibaba.druid.pool.DruidAbstractDataSource;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.db.DbUrlParser;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.protection.jdbc.connection.DruidPoolConnection;
import com.jd.live.agent.plugin.protection.jdbc.connection.LiveDriverConnection;
import com.jd.live.agent.plugin.protection.jdbc.datasource.LiveDataSource;
import com.jd.live.agent.plugin.protection.jdbc.datasource.LiveDruidDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

/**
 * DruidCreateConnectionInterceptor
 */
public class DruidCreateConnectionInterceptor extends AbstractCreateConnectionInterceptor<DruidPoolConnection> {

    public DruidCreateConnectionInterceptor(PolicySupplier policySupplier,
                                            Application application,
                                            GovernanceConfig governanceConfig,
                                            Publisher<DatabaseEvent> publisher,
                                            Timer timer,
                                            Map<String, DbUrlParser> parsers) {
        super(policySupplier, application, governanceConfig, publisher, timer, parsers);
    }

    @Override
    protected DataSource getDataSource(ExecutableContext ctx) {
        return (DruidAbstractDataSource) ctx.getTarget();
    }

    @Override
    protected LiveDataSource build(DataSource dataSource) {
        DruidAbstractDataSource druid = ((DruidAbstractDataSource) dataSource);
        return new LiveDruidDataSource(druid, DbUrlParser.parse(druid.getUrl(), parsers::get));
    }

    @Override
    protected DruidPoolConnection build(Connection connection,
                                        ClusterRedirect address,
                                        LiveDriverConnection driver,
                                        Object target) {
        return new DruidPoolConnection(connection, address, driver, closer);
    }
}
