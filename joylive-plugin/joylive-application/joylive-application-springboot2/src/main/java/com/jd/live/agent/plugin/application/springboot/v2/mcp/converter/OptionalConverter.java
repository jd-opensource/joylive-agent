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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.converter;

import com.jd.live.agent.core.util.converter.Converter;

import java.util.Optional;

public class OptionalConverter implements Converter<Object, Object> {

    public static final OptionalConverter INSTANCE = new OptionalConverter();

    @Override
    public Object convert(Object source) {
        if (source == null) {
            return Optional.empty();
        } else if (source instanceof Optional) {
            return source;
        }
        return Optional.ofNullable(source);
    }
}
