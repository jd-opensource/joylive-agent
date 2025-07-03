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
package com.jd.live.agent.plugin.protection.mongodb.v4.request;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.request.DbRequest.SQLRequest;
import com.mongodb.MongoClientException;
import com.mongodb.ServerAddress;

public class MongodbRequest extends AbstractAttributes implements SQLRequest {

    private final ServerAddress serverAddress;

    private final String database;

    public MongodbRequest(ServerAddress serverAddress, String database) {
        this.serverAddress = serverAddress;
        this.database = database;
    }

    @Override
    public String getType() {
        return "mongodb";
    }

    @Override
    public String[] getAddresses() {
        return new String[]{serverAddress == null ? null : serverAddress.toString()};
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public String getSql() {
        return null;
    }

    @Override
    public AccessMode getAccessMode() {
        return AccessMode.READ_WRITE;
    }

    @Override
    public Exception reject(String message) {
        return new MongoClientException(message);
    }
}
