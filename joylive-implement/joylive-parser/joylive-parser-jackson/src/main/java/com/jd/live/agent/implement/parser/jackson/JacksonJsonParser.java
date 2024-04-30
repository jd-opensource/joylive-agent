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
package com.jd.live.agent.implement.parser.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.core.parser.ObjectParser;

import java.io.IOException;
import java.io.Reader;

@Extension(value = ObjectParser.JSON, provider = "jackson")
public class JacksonJsonParser extends AbstractJacksonParser implements JsonPathParser {

    @Override
    public <T> T read(Reader reader, String path) {
        if (reader == null || path == null) {
            return null;
        }
        try {
            JsonNode jsonNode = mapper.readTree(reader);
            return JsonPath.read(jsonNode, path);
        } catch (JsonPathException | IOException e) {
            throw new ParseException("failed to parse " + path, e);
        }
    }
}
