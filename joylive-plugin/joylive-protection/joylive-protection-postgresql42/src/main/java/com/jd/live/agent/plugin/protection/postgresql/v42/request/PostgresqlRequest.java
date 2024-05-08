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
package com.jd.live.agent.plugin.protection.postgresql.v42.request;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.governance.request.DbRequest.SQLRequest;
import org.postgresql.core.Query;
import org.postgresql.core.QueryExecutor;

public class PostgresqlRequest extends AbstractAttributes implements SQLRequest {

    private final QueryExecutor executor;
    private final Query query;

    public PostgresqlRequest(QueryExecutor executor, Query query) {
        this.executor = executor;
        this.query = query;
    }

    @Override
    public String getHost() {
        return executor.getHostSpec().getHost();
    }

    @Override
    public int getPort() {
        return executor.getHostSpec().getPort();
    }

    @Override
    public String getDatabase() {
        return executor.getDatabase();
    }

    @Override
    public String getSql() {
        return query.toString();
    }
}
