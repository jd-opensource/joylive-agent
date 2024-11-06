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
package com.jd.live.agent.implement.parser.fastjson2;

import com.alibaba.fastjson2.JSONPath;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.core.util.IOUtils;

import java.io.InputStream;

@Extension(value = "fastjson2", order = 1)
public class Fastjson2JsonPathParser implements JsonPathParser {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T read(String reader, String path) {
        try {
            return (T) JSONPath.eval(reader, path);
        } catch (Throwable e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T read(InputStream in, String path) {
        try {
            byte[] buffer = IOUtils.read(in);
            return (T) JSONPath.eval(new String(buffer), path);
        } catch (Throwable e) {
            throw new ParseException(e.getMessage(), e);
        }
    }
}

