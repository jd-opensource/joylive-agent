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
package com.jd.live.agent.plugin.protection.redisson.v3.request;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.request.DbRequest.CacheRequest;
import com.jd.live.agent.plugin.protection.redisson.v3.config.AddressResolverFactory;
import org.redisson.client.RedisException;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.connection.ConnectionManager;

import java.util.function.Function;

public class RedissonRequest extends AbstractAttributes implements CacheRequest {

    private final RedisCommand<?> command;

    private final String[] addresses;

    private final Function<String, AccessMode> commandFunc;

    public RedissonRequest(ConnectionManager connectionManager, RedisCommand<?> command, Function<String, AccessMode> commandFunc) {
        this.command = command;
        this.commandFunc = commandFunc;
        this.addresses = AddressResolverFactory.getAddresses(connectionManager);
    }

    @Override
    public String getType() {
        return "redis";
    }

    @Override
    public String[] getAddresses() {
        return addresses;
    }

    @Override
    public String getDatabase() {
        return null;
    }

    @Override
    public AccessMode getAccessMode() {
        return commandFunc.apply(command.getName());
    }

    @Override
    public Exception reject(String message) {
        return new RedisException(message);
    }

}
