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
package com.jd.live.agent.plugin.protection.jedis.v5.request;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.request.DbRequest.CacheRequest;
import com.jd.live.agent.plugin.protection.jedis.v5.config.JedisHostAndPortMapper;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisException;

import java.net.Socket;
import java.util.function.Function;

public class JedisRequest extends AbstractAttributes implements CacheRequest {

    private final Connection connection;

    private final ProtocolCommand command;

    private final String[] addresses;

    private final Function<String, AccessMode> commandFunc;

    public JedisRequest(Connection connection, ProtocolCommand command, Function<String, AccessMode> commandFunc) {
        this.connection = connection;
        this.command = command;
        this.addresses = parseAddresses(connection);
        this.commandFunc = commandFunc;
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
        if (command instanceof Protocol.Command) {
            return commandFunc.apply(((Protocol.Command) command).name());
        }
        return AccessMode.READ_WRITE;
    }

    @Override
    public Exception reject(String message) {
        return new JedisException(message);
    }

    /**
     * Parses the connection to extract host and port information.
     *
     * @param connection the connection to parse
     * @return HostAndPort containing the connection details
     */
    private String[] parseAddresses(Connection connection) {
        JedisSocketFactory socketFactory = (JedisSocketFactory) Accessor.socketFactory.get(connection);
        if (socketFactory instanceof DefaultJedisSocketFactory) {
            JedisHostAndPortMapper mapper = Accessor.hostAndPortMapper.get(socketFactory, JedisHostAndPortMapper.class);
            if (mapper != null) {
                return mapper.getAddresses();
            }
            return new String[]{((DefaultJedisSocketFactory) socketFactory).getHostAndPort().toString()};
        }
        Socket socket = (Socket) Accessor.socket.get(connection);
        return new String[]{new HostAndPort(socket.getInetAddress().getHostAddress(), socket.getPort()).toString()};
    }

    private static class Accessor {

        private static final FieldAccessor socketFactory = FieldAccessorFactory.getAccessor(Connection.class, "socketFactory");
        private static final FieldAccessor socket = FieldAccessorFactory.getAccessor(Connection.class, "socket");
        private static final FieldAccessor hostAndPortMapper = FieldAccessorFactory.getAccessor(DefaultJedisSocketFactory.class, "hostAndPortMapper");
    }
}
