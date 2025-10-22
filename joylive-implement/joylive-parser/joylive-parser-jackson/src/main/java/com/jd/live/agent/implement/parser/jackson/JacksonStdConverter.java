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

import com.fasterxml.jackson.databind.util.StdConverter;
import com.jd.live.agent.core.parser.json.JsonConverter;

/**
 * JacksonConverter is a custom converter that extends StdConverter to convert objects of type S to type T.
 * It uses a provided JsonConverter to perform the actual conversion.
 *
 * @param <S> the source type to convert from.
 * @param <T> the target type to convert to.
 */
public class JacksonStdConverter<S, T> extends StdConverter<S, T> {

    private final JsonConverter<S, T> converter;

    public JacksonStdConverter(JsonConverter<S, T> converter) {
        this.converter = converter;
    }

    @Override
    public T convert(S src) {
        return converter.convert(src);
    }
}
