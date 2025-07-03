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
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.request.DbRequest;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisException;

import java.lang.reflect.Method;

public class JedisRequest extends AbstractAttributes implements DbRequest.CacheRequest {

    private final Connection connection;

    private final CommandArguments args;

    private final HostAndPort hostAndPort;

    public JedisRequest(Connection connection, CommandArguments args) {
        this.connection = connection;
        this.args = args;
        JedisSocketFactory socketFactory = (JedisSocketFactory) Accessor.socketFactory.get(connection);
        // TODO hostAndPort
        HostAndPort hostAndPort = null;
        try {
            hostAndPort = socketFactory == null ? null : (HostAndPort) Accessor.getSocketHostAndPort.invoke(socketFactory);
        } catch (Throwable ignore) {
        }
        this.hostAndPort = hostAndPort;
    }

    @Override
    public String getType() {
        return "redis";
    }

    @Override
    public String getHost() {
        return hostAndPort.getHost();
    }

    @Override
    public int getPort() {
        return hostAndPort.getPort();
    }

    @Override
    public String getDatabase() {
        return null;
    }

    @Override
    public AccessMode getAccessMode() {
        ProtocolCommand command = args.getCommand();
        if (command instanceof Protocol.Command) {
            switch ((Protocol.Command) command) {
                case AUTH:
                case HELLO:
                case PING:
                case SELECT:
                case SWAPDB:
                case DBSIZE:
                case FLUSHDB:
                case FLUSHALL:
                case TIME:
                case CONFIG:
                case INFO:
                case SHUTDOWN:
                case MONITOR:
                case SLAVEOF:
                case REPLICAOF:
                case CLIENT:
                case ASKING:
                case READONLY:
                case READWRITE:
                case CLUSTER:
                case SENTINEL:
                case MODULE:
                case ACL:
                case LASTSAVE:
                case SAVE:
                case BGSAVE:
                case BGREWRITEAOF:
                case ROLE:
                case FAILOVER:
                case SLOWLOG:
                case LOLWUT:
                case COMMAND:
                case RESET:
                case LATENCY:
                case WAIT:
                case WAITAOF:
                case MULTI:
                case DISCARD:
                case WATCH:
                case UNWATCH:
                    return AccessMode.NONE;
                // Read-only commands
                case GET:
                case GETEX:
                case GETDEL:
                case GETBIT:
                case GETRANGE:
                case MGET:
                case HGET:
                case HMGET:
                case HGETALL:
                case HSTRLEN:
                case HEXISTS:
                case HLEN:
                case HKEYS:
                case HVALS:
                case HRANDFIELD:
                case EXISTS:
                case TYPE:
                case STRLEN:
                case LLEN:
                case LRANGE:
                case LINDEX:
                case LPOS:
                case SCARD:
                case SMEMBERS:
                case SISMEMBER:
                case SMISMEMBER:
                case SRANDMEMBER:
                case ZCARD:
                case ZCOUNT:
                case ZLEXCOUNT:
                case ZRANGE:
                case ZRANGEBYLEX:
                case ZRANGEBYSCORE:
                case ZRANK:
                case ZREVRANGE:
                case ZREVRANGEBYLEX:
                case ZREVRANGEBYSCORE:
                case ZREVRANK:
                case ZSCORE:
                case ZMSCORE:
                case XLEN:
                case XRANGE:
                case XREVRANGE:
                case XINFO:
                case PFCOUNT:
                case BITCOUNT:
                case BITPOS:
                case BITFIELD_RO:
                case LCS:
                case GEODIST:
                case GEOHASH:
                case GEOPOS:
                case GEORADIUS_RO:
                case GEOSEARCH:
                case GEORADIUSBYMEMBER_RO:
                    return AccessMode.READ;
                default:
                    return AccessMode.READ_WRITE;
            }
        }
        return AccessMode.NONE;
    }

    @Override
    public Exception reject(String message) {
        return new JedisException(message);
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor socketFactory = UnsafeFieldAccessorFactory.getAccessor(Connection.class, "socketFactory");

        private static final Method getSocketHostAndPort = ClassUtils.getDeclaredMethod(JedisSocketFactory.class, "getSocketHostAndPort");

    }
}
