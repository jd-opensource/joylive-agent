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
package com.jd.live.agent.plugin.protection.postgresql.v9_4.request;

import com.jd.live.agent.bootstrap.util.AttributeAccessorSupport;
import com.jd.live.agent.governance.request.DbRequest.SQLRequest;
import org.postgresql.core.ProtocolConnection;
import org.postgresql.core.Query;

public class PostgresqlRequest extends AttributeAccessorSupport implements SQLRequest {

    private final ProtocolConnection connection;
    private final Query query;

    public PostgresqlRequest(ProtocolConnection connection, Query query) {
        this.connection = connection;
        this.query = query;
    }

    @Override
    public String getHost() {
        return connection.getHostSpec().getHost();
    }

    @Override
    public int getPort() {
        return connection.getHostSpec().getPort();
    }

    @Override
    public String getDatabase() {
        return connection.getDatabase();
    }

    @Override
    public String getSql() {
        return query.toString();
    }
}
