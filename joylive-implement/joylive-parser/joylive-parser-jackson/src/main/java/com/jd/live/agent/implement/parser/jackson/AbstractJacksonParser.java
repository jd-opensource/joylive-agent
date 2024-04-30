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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

public abstract class AbstractJacksonParser implements ConfigParser, ObjectParser {

    protected ObjectMapper mapper;

    public AbstractJacksonParser() {
        mapper = configure(new ObjectMapper(createFactory())).registerModules(
                ObjectMapper.findModules(AbstractJacksonParser.class.getClassLoader()));
    }

    protected JsonFactory createFactory() {
        return null;
    }

    protected ObjectMapper configure(ObjectMapper mapper) {
        return mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
                setSerializationInclusion(JsonInclude.Include.NON_NULL).
                setAnnotationIntrospector(new JsonAnnotationIntrospector());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> parse(Reader reader) {
        return read(reader, Map.class);
    }

    @Override
    public <T> T read(Reader reader, Class<T> clazz) {
        if (reader == null || clazz == null) {
            return null;
        }
        try {
            return mapper.readValue(reader, clazz);
        } catch (IOException e) {
            throw new ParseException("read error. caused by " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T read(Reader reader, TypeReference<T> reference) {
        if (reader == null || reference == null) {
            return null;
        }
        try {
            return (T) mapper.readValue(reader, new SimpleTypeReference(reference.getType()));
        } catch (IOException e) {
            throw new ParseException("read error. caused by " + e.getMessage(), e);
        }
    }

    @Override
    public void write(Writer writer, Object obj) {
        if (writer != null && obj != null) {
            try {
                mapper.writer().writeValue(writer, obj);
            } catch (IOException e) {
                throw new ParseException("write error. caused by " + e.getMessage(), e);
            }
        }
    }
}
