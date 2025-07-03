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
import com.jd.live.agent.governance.request.DbRequest.CacheRequest;
import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.*;

import java.util.function.Function;

public abstract class LettuceRequest extends AbstractAttributes implements CacheRequest {

    protected final String[] addresses;

    protected final Function<String, AccessMode> commandFunc;

    public LettuceRequest(String[] addresses, Function<String, AccessMode> commandFunc) {
        this.addresses = addresses;
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
    public Exception reject(String message) {
        return new RedisException(message);
    }

    protected AccessMode getAccessMode(RedisCommand<?, ?, ?> command) {
        while (command instanceof DecoratedCommand<?, ?, ?>) {
            command = ((DecoratedCommand<?, ?, ?>) command).getDelegate();
        }
        if (command instanceof Command<?, ?, ?>) {
            ProtocolKeyword keyword = command.getType();
            if (keyword instanceof CommandType) {
                return commandFunc.apply(((CommandType) keyword).name());
            }
        }
        return AccessMode.READ_WRITE;
    }

}
