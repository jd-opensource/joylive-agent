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
package com.jd.live.agent.plugin.protection.mariadb.v3.request;

import com.jd.live.agent.governance.request.AbstractDbRequest;
import com.jd.live.agent.governance.request.DbRequest.SQLRequest;
import org.mariadb.jdbc.client.Context;
import org.mariadb.jdbc.client.impl.StandardClient;
import org.mariadb.jdbc.message.ClientMessage;

public class MariadbRequest extends AbstractDbRequest implements SQLRequest {

    private final StandardClient client;

    private final ClientMessage request;

    public MariadbRequest(StandardClient client, ClientMessage request) {
        this.client = client;
        this.request = request;
    }

    @Override
    public String getName() {
        String name = client.getContext().getConf().nonMappedOptions().getProperty(KEY_CLUSTER);
        return name != null ? name : SQLRequest.super.getName();
    }

    @Override
    public String getHost() {
        return client.getHostAddress().host;
    }

    @Override
    public int getPort() {
        return client.getHostAddress().port;
    }

    @Override
    public String getDatabase() {
        Context context = client.getContext();
        return context == null ? null : context.getDatabase();
    }

    @Override
    public String getSql() {
        return request.description();
    }

}
