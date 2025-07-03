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
package com.jd.live.agent.plugin.protection.lettuce.v6.request;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.request.DbRequest;
import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.*;

import java.util.function.Function;

public class LettuceRequest extends AbstractAttributes implements DbRequest.CacheRequest {

    private final RedisCommand<?, ?, ?> command;

    private final Function<String, AccessMode> commandFunc;

    public LettuceRequest(RedisCommand<?, ?, ?> command, Function<String, AccessMode> commandFunc) {
        this.command = command;
        this.commandFunc = commandFunc;
    }

    @Override
    public String getType() {
        return "redis";
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

    @Override
    public AccessMode getAccessMode() {
        RedisCommand<?, ?, ?> cmd = command;
        while (cmd instanceof DecoratedCommand<?, ?, ?>) {
            cmd = ((DecoratedCommand<?, ?, ?>) cmd).getDelegate();
        }
        if (cmd instanceof Command<?, ?, ?>) {
            ProtocolKeyword keyword = cmd.getType();
            if (keyword instanceof CommandType) {
                return commandFunc.apply(((CommandType) keyword).name());
            }
        }
        return AccessMode.READ_WRITE;
    }

    @Override
    public Exception reject(String message) {
        return new RedisException(message);
    }

}
