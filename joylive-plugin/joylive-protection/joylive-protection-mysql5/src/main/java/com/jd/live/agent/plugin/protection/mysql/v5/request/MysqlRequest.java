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
package com.jd.live.agent.plugin.protection.mysql.v5.request;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.governance.request.DbRequest.SQLRequest;
import com.mysql.jdbc.ConnectionImpl;
import com.mysql.jdbc.MySQLConnection;

import java.sql.SQLException;

public class MysqlRequest extends AbstractAttributes implements SQLRequest {

    private static final UnsafeFieldAccessor accessor = UnsafeFieldAccessorFactory.getAccessor(ConnectionImpl.class, "port");

    private final MySQLConnection connection;

    private final String sql;

    public MysqlRequest(MySQLConnection connection, String sql) {
        this.connection = connection;
        this.sql = sql;
    }

    @Override
    public String getHost() {
        return connection.getHost();
    }

    @Override
    public int getPort() {
        return accessor.getInt(connection);
    }

    @Override
    public String getDatabase() {
        try {
            return connection.getCatalog();
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public String getSql() {
        return sql;
    }
}
