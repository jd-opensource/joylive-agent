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

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.modules.ObjectReaderAnnotationProcessor;
import com.alibaba.fastjson2.modules.ObjectReaderModule;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.util.BeanUtils;
import com.jd.live.agent.core.parser.json.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import static com.jd.live.agent.implement.parser.fastjson2.Converters.getConverter;

/**
 * A module for the ObjectReader that handles custom annotations for JSON deserialization.
 */
public class JoyLiveReaderModule implements ObjectReaderModule {

    public JoyLiveReaderModule() {
    }

    public ObjectReaderAnnotationProcessor getAnnotationProcessor() {
        return JoyLiveReadAnnotationProcessor.INSTANCE;
    }

    /**
     * A private static class that implements the ObjectReaderAnnotationProcessor interface.
     */
    private static class JoyLiveReadAnnotationProcessor implements ObjectReaderAnnotationProcessor {

        private static final JoyLiveReadAnnotationProcessor INSTANCE = new JoyLiveReadAnnotationProcessor();

        @Override
        public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Field field) {
            processAnnotation(fieldInfo, BeanUtils.getAnnotations(field), field);
        }

        /**
         * Processes the annotations for a given field.
         *
         * @param fieldInfo   The field information.
         * @param annotations The annotations on the field.
         * @param field       The field being processed.
         */
        private void processAnnotation(FieldInfo fieldInfo, Annotation[] annotations, Field field) {
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                String annotationTypeName = annotationType.getName();
                switch (annotationTypeName) {
                    case "com.jd.live.agent.core.parser.json.JsonField":
                        processJsonField(fieldInfo, (JsonField) annotation);
                        break;
                    case "com.jd.live.agent.core.parser.json.JsonAlias":
                        processJsonAlias(fieldInfo, (JsonAlias) annotation);
                        break;
                    case "com.jd.live.agent.core.parser.json.JsonFormat":
                        processJsonFormat(fieldInfo, (JsonFormat) annotation, field);
                        break;
                    case "com.jd.live.agent.core.parser.json.DeserializeConverter":
                        processDeserializerConverter(fieldInfo, (DeserializeConverter) annotation, field);
                        break;
                }
            }
        }

        /**
         * Processes the JsonField annotation on a field.
         *
         * @param fieldInfo The field information.
         * @param jsonField The JsonField annotation.
         */
        private void processJsonField(FieldInfo fieldInfo, JsonField jsonField) {
            fieldInfo.fieldName = !jsonField.value().isEmpty() ? jsonField.value() : fieldInfo.fieldName;
        }

        /**
         * Processes the JsonAlias annotation on a field.
         *
         * @param fieldInfo The field information.
         * @param jsonAlias The JsonAlias annotation.
         */
        private void processJsonAlias(FieldInfo fieldInfo, JsonAlias jsonAlias) {
            String[] values = jsonAlias.value();
            if (values.length > 0) {
                String[] alternateNames = fieldInfo.alternateNames;
                if (alternateNames == null || alternateNames.length == 0) {
                    fieldInfo.alternateNames = values;
                } else {
                    String[] newValues = new String[alternateNames.length + values.length];
                    System.arraycopy(alternateNames, 0, newValues, 0, alternateNames.length);
                    System.arraycopy(values, 0, newValues, alternateNames.length, values.length);
                    fieldInfo.alternateNames = newValues;
                }
            }
        }

        /**
         * Processes the JsonFormat annotation on a field.
         *
         * @param fieldInfo  The field information.
         * @param jsonFormat The JsonFormat annotation.
         * @param field      The field being processed.
         */
        private void processJsonFormat(FieldInfo fieldInfo, JsonFormat jsonFormat, Field field) {
            fieldInfo.format = jsonFormat.pattern();
        }

        /**
         * Processes the DeserializeConverter annotation on a field.
         *
         * @param fieldInfo            The field information.
         * @param deserializeConverter The DeserializeConverter annotation.
         * @param field                The field being processed.
         */
        private void processDeserializerConverter(FieldInfo fieldInfo, DeserializeConverter deserializeConverter, Field field) {
            Class<? extends JsonConverter<?, ?>> clazz = deserializeConverter.value();
            String name = field.getName() + "@" + field.getType().getTypeName();
            Converters.getOrCreateConverter(name, clazz);
            fieldInfo.readUsing = ProxyObjectReader.class;
        }
    }

    /**
     * A proxy object reader that uses custom converters for deserialization.
     */
    public static class ProxyObjectReader implements ObjectReader<Object> {

        @SuppressWarnings("unchecked")
        @Override
        public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            String name = fieldName.toString() + "@" + fieldType.getTypeName();
            JsonConverter<Object, Object> jsonConverter = (JsonConverter<Object, Object>) getConverter(name);
            if (jsonConverter != null) {
                return jsonConverter.convert(jsonReader.readAny());
            }
            return null;
        }
    }
}

