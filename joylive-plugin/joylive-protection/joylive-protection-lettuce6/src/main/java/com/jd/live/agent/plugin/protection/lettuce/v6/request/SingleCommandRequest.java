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

import com.jd.live.agent.governance.policy.AccessMode;
import io.lettuce.core.protocol.RedisCommand;

import java.util.function.Function;

public class SingleCommandRequest extends LettuceRequest {

    private final RedisCommand<?, ?, ?> command;

    public SingleCommandRequest(RedisCommand<?, ?, ?> command, Function<String, AccessMode> commandFunc) {
        super(commandFunc);
        this.command = command;
    }

    @Override
    public AccessMode getAccessMode() {
        return getAccessMode(command);
    }

}
