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

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.JsonPathParser;

import java.io.IOException;
import java.io.InputStream;

@Extension(value = "jackson")
public class JacksonJsonPathParser implements JsonPathParser {

    @Override
    public <T> T read(String reader, String path) {
        if (reader == null || path == null) {
            return null;
        }
        try {
            return JsonPath.read(reader, path);
        } catch (JsonPathException e) {
            throw new ParseException("failed to parse " + path, e);
        }
    }

    @Override
    public <T> T read(InputStream in, String path) {
        if (in == null || path == null) {
            return null;
        }
        try {
            return JsonPath.read(in, path);
        } catch (JsonPathException | IOException e) {
            throw new ParseException("failed to parse " + path, e);
        }
    }
}
