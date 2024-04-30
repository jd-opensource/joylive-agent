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
package com.jd.live.agent.core.inject.jbind;

import com.jd.live.agent.core.util.type.ObjectAccessor;
import com.jd.live.agent.core.util.type.generic.Generic;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an injection type, which is a descriptor for a class that can be used in dependency injection.
 * It contains information about the type and any associated annotation, and can hold a list of fields
 * that are annotated for injection.
 */
@Getter
public class InjectType {

    private final Class<?> type; // The class type that this injection type represents
    private final Annotation annotation; // The annotation associated with this type, if any
    private List<InjectField> fields; // A list of fields that are annotated for injection

    /**
     * Constructs a new InjectType instance for the specified class type.
     *
     * @param type the Class object representing the type
     */
    public InjectType(Class<?> type) {
        this(type, null);
    }

    /**
     * Constructs a new InjectType instance for the specified class type and annotation.
     *
     * @param type the Class object representing the type
     * @param annotation the annotation associated with the type, or null if there is none
     */
    public InjectType(Class<?> type, Annotation annotation) {
        this.type = type;
        this.annotation = annotation;
    }

    /**
     * Adds an InjectField to the list of fields for this injection type.
     *
     * @param field the InjectField to add
     */
    public void add(InjectField field) {
        if (field != null) {
            if (fields == null) fields = new ArrayList<>(5);
            fields.add(field);
        }
    }

    /**
     * Returns the number of InjectField instances associated with this injection type.
     *
     * @return the number of fields
     */
    public int size() {
        return fields == null ? 0 : fields.size();
    }

    /**
     * Determines if this injection type has no associated InjectField instances.
     *
     * @return true if there are no fields, false otherwise
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * A class representing an injectable field within an InjectType.
     * It contains information about the field, the key for injection, the accessor for field values,
     * any annotation on the field, generic type information, and a sourcer for obtaining field values.
     *
     * @since 1.0.0
     */
    public static class InjectField implements InjectScope<Field>, Sourcer {
        @Getter
        private final Field field;
        @Getter
        private final String key;
        private final ObjectAccessor accessor;
        private final Annotation annotation;
        private final Generic generic;
        @Getter
        private final Sourcer sourcer;

        /**
         * Constructs a new InjectField instance.
         *
         * @param field the reflective Field object
         * @param key the key for injection
         * @param accessor the accessor for field values
         * @param annotation the annotation on the field, if any
         * @param generic the generic type information for the field
         * @param sourcer the sourcer for obtaining field values
         */
        public InjectField(Field field, String key, ObjectAccessor accessor, Annotation annotation,
                           Generic generic, Sourcer sourcer) {
            this.field = field;
            this.key = key;
            this.accessor = accessor;
            this.annotation = annotation;
            this.generic = generic;
            this.sourcer = sourcer;
        }

        @Override
        public String getName() {
            return field.getName();
        }

        @Override
        public Annotation[] getAnnotations() {
            return field.getAnnotations();
        }

        @Override
        public Annotation getAnnotation() {
            return annotation;
        }

        @Override
        public Class<?> getType() {
            return generic != null ? generic.getErasure() : field.getType();
        }

        public Type getGenericType() {
            return generic != null ? generic.getType() : field.getGenericType();
        }

        @Override
        public Field getTarget() {
            return field;
        }

        @Override
        public Generic getGeneric() {
            return generic;
        }

        @Override
        public Object get(Object target) {
            return accessor == null ? null : accessor.get(target);
        }

        @Override
        public void set(Object target, Object value) {
            if (target != null && accessor != null) accessor.set(target, value);
        }

        @Override
        public Object getSource(Object context) {
            return sourcer == null ? null : sourcer.getSource(context);
        }

    }
}
