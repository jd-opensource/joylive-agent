package com.jd.live.agent.implement.parser.fastjson2;

import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.modules.ObjectReaderAnnotationProcessor;
import com.alibaba.fastjson2.modules.ObjectReaderModule;
import com.alibaba.fastjson2.util.BeanUtils;
import com.jd.live.agent.core.parser.json.*;
import com.jd.live.agent.implement.parser.fastjson2.proxy.object.ProxyObjectReader;
import com.jd.live.agent.implement.parser.fastjson2.proxy.time.ProxyTimeReader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Stack;

import static com.alibaba.fastjson2.util.BeanUtils.getAnnotations;

public class JoyLiveReaderModule implements ObjectReaderModule {

    final JoyLiveReadAnnotationProcessor annotationProcessor;

    public JoyLiveReaderModule() {
        this.annotationProcessor = new JoyLiveReadAnnotationProcessor();
    }

    public ObjectReaderAnnotationProcessor getAnnotationProcessor() {
        return annotationProcessor;
    }

    public class JoyLiveReadAnnotationProcessor implements ObjectReaderAnnotationProcessor {
        @Override
        public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Method method) {
            String methodName = method.getName();
            String fieldName;

            if (methodName.startsWith("set")) {
                fieldName = BeanUtils.setterName(methodName, null);
            } else {
                fieldName = BeanUtils.getterName(methodName, null);
            }

            BeanUtils.declaredFields(objectClass, field -> {
                if (field.getName().equals(fieldName)) {
                    int modifiers = field.getModifiers();
                    if (!Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                        Annotation[] annotations = getAnnotations(field);
                        processAnnotation(fieldInfo, annotations, field);
                    }
                }
            });
        }

        @Override
        public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Field field) {
            Annotation[] annotations = getAnnotations(field);
            processAnnotation(fieldInfo, annotations, field);
        }

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

        private void processJsonField(FieldInfo fieldInfo, JsonField jsonField) {
            String jsonFieldName = jsonField.value();
            if (!jsonFieldName.isEmpty()) {
                fieldInfo.fieldName = jsonFieldName;
            }
        }

        private void processJsonAlias(FieldInfo fieldInfo, JsonAlias jsonAlias) {
            String[] values = jsonAlias.value();
            if (values.length > 0) {
                fieldInfo.alternateNames = values;
            }
        }

        private void processJsonFormat(FieldInfo fieldInfo, JsonFormat jsonFormat, Field field) {
            String pattern = jsonFormat.pattern();
            String timezone = jsonFormat.timezone();
            String typeName = field.getType().getName();
            String fieldName = field.getName();
            boolean typeMatch = "java.util.Date".equals(typeName) ||
                    "java.util.Calendar".equals(typeName) ||
                    "java.time.ZonedDateTime".equals(typeName) ||
                    "java.time.OffsetDateTime".equals(typeName) ||
                    "java.time.OffsetTime".equals(typeName);
            if (typeMatch) {
                String cachedKey = field.getDeclaringClass().getName() + "." + field.getName();
                if (!ProxySupport.cachedKey.get().contains(cachedKey)) {
                    ProxySupport.timeFormatThreadLocal.get().computeIfAbsent(fieldName, key -> new Stack<>())
                            .push(new String[]{pattern, timezone});
                    ProxySupport.cachedKey.get().add(cachedKey);
                }
                fieldInfo.readUsing = ProxyTimeReader.class;
            } else {
                fieldInfo.format = pattern;
            }
        }

        private void processDeserializerConverter(FieldInfo fieldInfo, DeserializeConverter deserializeConverter, Field field) {
            try {
                Class<? extends JsonConverter<?, ?>> clazz = deserializeConverter.value();
                String className = clazz.getName();

                ProxySupport.jsonConverterThreadLocal.get().computeIfAbsent(field.getName(), key -> new Stack<>())
                        .push(
                                ProxySupport.converterMap.computeIfAbsent(className, key -> {
                                    try {
                                        Constructor<? extends JsonConverter<?, ?>> constructor = clazz.getConstructor();
                                        constructor.setAccessible(true);
                                        return constructor.newInstance();
                                    } catch (Exception e) {
                                        throw new RuntimeException(e.getMessage(), e);
                                    }
                                })
                        );
                fieldInfo.readUsing = ProxyObjectReader.class;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}

