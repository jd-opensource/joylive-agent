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

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

@Extension(value = ObjectParser.JSON, provider = "fastjson2")
public class Fastjson2JsonParser implements ObjectParser {

    static {
        JSONFactory.getDefaultObjectWriterProvider().register(new JoyLiveWriterModule());
        JSONFactory.getDefaultObjectReaderProvider().register(new JoyLiveReaderModule());
    }

    @Override
    public <T> T read(Reader reader, Class<T> clazz) {
        try {
            return com.alibaba.fastjson2.JSON.parseObject(reader, clazz, JSONReader.Feature.FieldBased);
        } catch (Exception e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    @Override
    public <T> T read(Reader reader, TypeReference<T> reference) {
        try {
            return com.alibaba.fastjson2.JSON.parseObject(reader, reference.getType(), JSONReader.Feature.FieldBased);
        } catch (Exception e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    @Override
    public <T> T read(Reader reader, Type type) {
        try {
            return com.alibaba.fastjson2.JSON.parseObject(reader, type, JSONReader.Feature.FieldBased);
        } catch (Exception e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    @Override
    public void write(Writer writer, Object obj) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1000);
        try {
            com.alibaba.fastjson2.JSON.writeTo(stream, obj, JSONWriter.Feature.FieldBased);
            writer.write(stream.toString());
        } catch (Throwable e) {
            throw new ParseException(e.getMessage(), e);
        }
    }
}

