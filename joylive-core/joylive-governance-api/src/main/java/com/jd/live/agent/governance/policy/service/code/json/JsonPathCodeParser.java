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
package com.jd.live.agent.governance.policy.service.code.json;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.governance.policy.service.code.CodeParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

@Injectable
@Extension("JsonPath")
public class JsonPathCodeParser implements CodeParser {

    @Inject
    private JsonPathParser parser;

    @Override
    public String getCode(String expression, Object response) {
        if (expression == null || expression.isEmpty() || response == null) {
            return null;
        }
        Reader reader;
        if (response instanceof String) {
            reader = new StringReader((String) response);
        } else if (response instanceof byte[]) {
            reader = new StringReader(new String((byte[]) response));
        } else if (response instanceof Reader) {
            reader = (Reader) response;
        } else if (response instanceof InputStream) {
            reader = new InputStreamReader((InputStream) response);
        } else {
            reader = new StringReader(response.toString());
        }

        return parser.read(reader, expression);
    }
}
