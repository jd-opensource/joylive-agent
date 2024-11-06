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

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.BeanInfo;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.modules.ObjectWriterAnnotationProcessor;
import com.alibaba.fastjson2.modules.ObjectWriterModule;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.jd.live.agent.core.parser.json.JsonConverter;
import com.jd.live.agent.core.parser.json.JsonField;
import com.jd.live.agent.core.parser.json.JsonFormat;
import com.jd.live.agent.core.parser.json.SerializeConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import static com.alibaba.fastjson2.util.BeanUtils.getAnnotations;
import static com.jd.live.agent.implement.parser.fastjson2.Converters.getConverter;
import static com.jd.live.agent.implement.parser.fastjson2.Converters.getOrCreateConverter;

/**
 * A module for the ObjectWriter that handles custom annotations for JSON serialization.
 */
public class JoyLiveWriterModule implements ObjectWriterModule {

    /**
     * Gets the annotation processor for this module.
     *
     * @return The annotation processor instance.
     */
    public ObjectWriterAnnotationProcessor getAnnotationProcessor() {
        return JoyLiveWriteAnnotationProcessor.INSTANCE;
    }

    /**
     * A private static class that implements the ObjectWriterAnnotationProcessor interface.
     */
    private static class JoyLiveWriteAnnotationProcessor implements ObjectWriterAnnotationProcessor {

        private static final JoyLiveWriteAnnotationProcessor INSTANCE = new JoyLiveWriteAnnotationProcessor();

        @Override
        public void getFieldInfo(BeanInfo beanInfo, FieldInfo fieldInfo, Class objectClass, Field field) {
            processAnnotation(fieldInfo, getAnnotations(field), field);
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
                    case "com.jd.live.agent.core.parser.json.JsonFormat":
                        processJsonFormat(fieldInfo, (JsonFormat) annotation, field);
                        break;
                    case "com.jd.live.agent.core.parser.json.SerializeConverter":
                        processSerializerConverter(fieldInfo, (SerializeConverter) annotation, field);
                        break;
                    default:
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
         * Processes the SerializeConverter annotation on a field.
         *
         * @param fieldInfo          The field information.
         * @param serializeConverter The SerializeConverter annotation.
         * @param field              The field being processed.
         */
        private void processSerializerConverter(FieldInfo fieldInfo, SerializeConverter serializeConverter, Field field) {
            Class<? extends JsonConverter<?, ?>> clazz = serializeConverter.value();
            String name = field.getName() + "@" + field.getType().getTypeName();
            getOrCreateConverter(name, clazz);
            fieldInfo.writeUsing = ProxyObjectWriter.class;
        }
    }

    /**
     * A proxy object writer that uses custom converters for serialization.
     */
    public static class ProxyObjectWriter implements ObjectWriter<Object> {

        @SuppressWarnings("unchecked")
        @Override
        public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
            String name = fieldName.toString() + "@" + fieldType.getTypeName();
            JsonConverter<Object, Object> converter = (JsonConverter<Object, Object>) getConverter(name);
            if (converter != null) {
                jsonWriter.writeAny(converter.convert(object));
            } else {
                jsonWriter.writeNull();
            }
        }
    }
}

