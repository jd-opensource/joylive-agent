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
package com.jd.live.agent.core.util.type;

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.type.generic.Generic;
import com.jd.live.agent.core.util.type.generic.GenericType;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * ClassDesc represents metadata about a class, including its fields, constructors, methods, and generic information.
 * It also provides access to the codebase location and associated artifact information if available.
 */
public class ClassDesc {
    /**
     * The class type represented by this ClassDesc.
     */
    @Getter
    protected final Class<?> type;
    /**
     * Metadata about the fields of the class, lazily initialized.
     */
    protected final LazyObject<FieldList> fieldList;
    /**
     * Metadata about the constructors of the class, lazily initialized.
     */
    protected final LazyObject<ConstructorList> constructorList;
    /**
     * Metadata about the methods of the class, lazily initialized.
     */
    protected final LazyObject<MethodList> methodList;
    /**
     * Information about the generic types used in the class, lazily initialized.
     */
    protected final LazyObject<GenericType> genericType;
    /**
     * The location of the codebase, which can be a file or a jar package, lazily initialized.
     */
    protected final LazyObject<String> codebase;
    /**
     * The artifact associated with the class, if any, lazily initialized.
     */
    protected final LazyObject<Artifact> artifact;
    /**
     * A supplier for accessing getters, setters, and generic information of fields.
     */
    protected final FieldSupplier supplier;

    /**
     * Constructs a ClassDesc object for the given class type.
     *
     * @param type The class type to describe.
     */
    public ClassDesc(final Class<?> type) {
        this.type = type;
        this.supplier = new FieldSupplier() {
            @Override
            public Method getGetter(Field field) {
                return methodList.get().getGetter(field.getName());
            }

            @Override
            public Method getSetter(Field field) {
                return methodList.get().getSetter(field.getName());
            }

            public Generic getGeneric(Field field) {
                return genericType.get().get(field);
            }
        };
        this.fieldList = new LazyObject<>(() -> new FieldList(type, supplier));
        this.constructorList = new LazyObject<>(() -> new ConstructorList(type));
        this.methodList = new LazyObject<>(() -> new MethodList(type, name -> {
            FieldDesc desc = fieldList.get().getField(name);
            return desc != null && !Modifier.isFinal(desc.getField().getModifiers());
        }));
        this.genericType = new LazyObject<>(() -> new GenericType(type));
        this.codebase = new LazyObject<>(this::loadCodeBase);
        this.artifact = new LazyObject<>(() -> new Artifact(codebase.get()));
    }

    /**
     * Gets the metadata about the constructors of the class.
     *
     * @return The ConstructorList containing constructor metadata.
     */
    public ConstructorList getConstructorList() {
        return constructorList.get();
    }

    public Object getValue(String fieldName, Object target) {
        FieldDesc fieldDesc = getFieldList().getField(fieldName);
        return fieldDesc == null ? null : fieldDesc.get(target);
    }

    /**
     * Retrieves the metadata of the fields.
     *
     * @return The metadata of the fields.
     */
    public FieldList getFieldList() {
        return fieldList.get();
    }

    /**
     * Retrieves the metadata for methods.
     *
     * @return An instance of MethodList containing method metadata.
     */
    public MethodList getMethodList() {
        return methodList.get();
    }

    /**
     * Retrieves the metadata of the methods.
     *
     * @return The metadata of the methods.
     */
    public GenericType getGenericType() {
        return genericType.get();
    }

    /**
     * Gets the location of the class, which can be a file or a jar package.
     *
     * @return The code base of the class.
     */
    public String getCodeBase() {
        return codebase.get();
    }

    public Artifact getArtifact() {
        return artifact.get();
    }

    /**
     * Loads the code base location of the current class. This can be a path to a file or a jar.
     * It utilizes the ProtectionDomain and CodeSource to determine the location.
     *
     * @return The file path or jar path where the class is located, or null if it cannot be determined.
     */
    private String loadCodeBase() {
        String file = null;
        ProtectionDomain domain = type.getProtectionDomain();
        if (domain != null) {
            CodeSource source = domain.getCodeSource();
            if (source != null) {
                URL location = source.getLocation();
                if (location != null) {
                    file = location.getFile();
                }
            }
        }
        return file;
    }

    /**
     * Instantiates a new object of type T.
     *
     * @param <T> The type of the object to be instantiated.
     * @return A new instance of type T.
     */
    public <T> T newInstance() {
        return getConstructorList().newInstance();
    }
}
