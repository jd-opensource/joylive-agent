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
package com.jd.live.agent.core.parser.jdk;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.JsonSchemaParser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Extension("reflection")
public class ReflectionJsonSchemaParser implements JsonSchemaParser {

    public static final JsonSchemaParser INSTANCE = new ReflectionJsonSchemaParser();

    @Override
    public List<FieldSchema> describe(Class<?> cls) {
        List<FieldSchema> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isFinal(field.getModifiers())
                        || Modifier.isTransient(field.getModifiers())
                        || !names.add(field.getName())) {
                    continue;
                }
                result.add(new FieldSchema(field.getName(), field, AccessType.READ_WRITE));
            }
            cls = cls.getSuperclass();
        }
        return result;
    }
}
