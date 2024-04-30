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
package com.jd.live.agent.governance.policy.variable.http;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.policy.HttpScope;
import com.jd.live.agent.governance.policy.variable.VariableParser;
import com.jd.live.agent.governance.policy.variable.VariableSource.HttpVariableSource;
import com.jd.live.agent.governance.request.HttpRequest;

@Extension(VariableParser.TYPE_HTTP)
public class HttpVariableParser implements VariableParser<HttpRequest, HttpVariableSource> {

    @Override
    public String parse(HttpRequest request, HttpVariableSource source) {
        String result = null;
        if (request != null && source != null) {
            HttpScope scope = source.getScope();
            String key = source.getKey();
            if (scope != null && key != null) {
                switch (scope) {
                    case QUERY:
                        result = request.getQuery(key);
                        break;
                    case COOKIE:
                        result = request.getCookie(key);
                        break;
                    case HEADER:
                    default:
                        result = request.getHeader(key);
                        break;
                }
            }
        }
        return result;
    }
}
