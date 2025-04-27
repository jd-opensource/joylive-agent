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
package com.jd.live.agent.plugin.protection.mysql.v6.request;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.governance.request.DbRequest.SQLRequest;
import com.mysql.cj.api.jdbc.JdbcConnection;

public class MysqlRequest extends AbstractAttributes implements SQLRequest {

    private final JdbcConnection connection;

    private final String sql;

    public MysqlRequest(JdbcConnection connection, String sql) {
        this.connection = connection;
        this.sql = sql;
    }

    @Override
    public String getHost() {
        return connection.getHost();
    }

    @Override
    public int getPort() {
        return connection.getSession().getHostInfo().getPort();
    }

    @Override
    public String getDatabase() {
        return connection.getSession().getHostInfo().getDatabase();
    }

    @Override
    public String getSql() {
        return sql;
    }
}
