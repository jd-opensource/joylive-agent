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
package com.jd.live.agent.governance.policy.variable.method;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.util.type.ValuePath;
import com.jd.live.agent.governance.policy.variable.VariableParser;
import com.jd.live.agent.governance.policy.variable.VariableSource.MethodVariableSource;
import com.jd.live.agent.governance.request.RpcRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Extension(VariableParser.TYPE_METHOD)
public class MethodVariableParser implements VariableParser<RpcRequest, MethodVariableSource> {

    private final Map<String, ValuePath> pathMap = new ConcurrentHashMap<>();

    @Override
    public String parse(RpcRequest request, MethodVariableSource source) {
        String result = null;
        if (request != null && source != null) {
            Object arg = request.getArgument(source.getArgument());
            if (arg != null) {
                String path = source.getPath();
                if (path == null || path.isEmpty()) {
                    return arg.toString();
                } else {
                    ValuePath valuePath = pathMap.computeIfAbsent(path, ValuePath::new);
                    arg = valuePath.get(arg);
                    return arg == null ? null : arg.toString();
                }
            }
        }
        return result;
    }
}
