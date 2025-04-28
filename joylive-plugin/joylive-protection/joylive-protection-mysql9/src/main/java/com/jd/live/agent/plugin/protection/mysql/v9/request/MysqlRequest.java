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
package com.jd.live.agent.plugin.protection.mysql.v9.request;

import com.jd.live.agent.governance.request.AbstractDbRequest;
import com.jd.live.agent.governance.request.DbRequest.SQLRequest;
import com.mysql.cj.Session;
import com.mysql.cj.conf.HostInfo;

public class MysqlRequest extends AbstractDbRequest implements SQLRequest {

    private final Session session;

    private final String sql;

    public MysqlRequest(Session session, String sql) {
        this.session = session;
        this.sql = sql;
    }

    @Override
    public String getHost() {
        HostInfo hostInfo = session.getHostInfo();
        return hostInfo.getHost();
    }

    @Override
    public int getPort() {
        HostInfo hostInfo = session.getHostInfo();
        return hostInfo.getPort();
    }

    @Override
    public String getDatabase() {
        HostInfo hostInfo = session.getHostInfo();
        return hostInfo.getDatabase();
    }

    @Override
    public String getSql() {
        return sql;
    }
}
