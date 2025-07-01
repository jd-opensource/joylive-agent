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
package com.jd.live.agent.plugin.protection.redis.request;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.governance.request.DbRequest;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;

public class JedisRequest extends AbstractAttributes implements DbRequest.CacheRequest {

    private final Connection connection;
    private final CommandArguments args;

    public JedisRequest(Connection connection, CommandArguments args) {
        this.connection = connection;
        this.args = args;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getDatabase() {
        return null;
    }
}
