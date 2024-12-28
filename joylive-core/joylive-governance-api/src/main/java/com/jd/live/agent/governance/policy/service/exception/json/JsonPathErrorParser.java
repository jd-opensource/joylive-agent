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
package com.jd.live.agent.governance.policy.service.exception.json;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.governance.policy.service.exception.ErrorParser;

import java.io.InputStream;

@Injectable
@Extension("JsonPath")
public class JsonPathErrorParser implements ErrorParser {

    @Inject
    private JsonPathParser parser;

    @Override
    public String getValue(String expression, Object response) {
        if (expression == null || expression.isEmpty() || response == null) {
            return null;
        }
        Object result;
        if (response instanceof String) {
            result = parser.read((String) response, expression);
        } else if (response instanceof byte[]) {
            result = parser.read(new String((byte[]) response), expression);
        } else if (response instanceof InputStream) {
            result = parser.read((InputStream) response, expression);
        } else {
            result = parser.read(response.toString(), expression);
        }
        return result == null ? null : result.toString();
    }
}
