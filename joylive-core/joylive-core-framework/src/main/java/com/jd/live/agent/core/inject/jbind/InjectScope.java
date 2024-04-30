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

import com.jd.live.agent.core.util.type.generic.Generic;

import java.lang.annotation.Annotation;

/**
 * Defines the contract for an injection scope, which is responsible for managing the lifecycle
 * and retrieval of objects that are to be injected into other components.
 *
 * <p>An injection scope typically defines how and when an object should be created, managed,
 * and disposed of within an application's component lifecycle. This can include singletons,
 * prototypes, or other custom-managed lifecycles.
 *
 * @param <T> the type of object that this scope is designed to manage
 * @since 1.0.0
 */
public interface InjectScope<T> {

    /**
     * Returns the name of this injection scope.
     *
     * @return the name of the scope
     */
    String getName();

    /**
     * Returns an array of annotations that are associated with this scope.
     * These annotations can be used to provide additional metadata or configuration
     * for the scope.
     *
     * @return an array of annotations
     */
    Annotation[] getAnnotations();

    /**
     * Returns a single annotation of the specified type that is associated with this scope.
     * If the annotation is not present, the method returns {@code null}.
     *
     * @return the annotation of the specified type, or {@code null} if it is not present
     */
    Annotation getAnnotation();

    /**
     * Returns the type of object that this scope is designed to manage.
     *
     * @return the Class object representing the type of the object
     */
    Class<?> getType();

    /**
     * Returns the target object that this scope is managing. This is the actual instance
     * that will be injected into other components.
     *
     * @return the target object
     */
    T getTarget();

    /**
     * Returns a representation of the generic type information for the object that this scope manages.
     * This can be useful for dealing with generic types in a type-safe way during injection.
     *
     * @return the generic type information
     */
    Generic getGeneric();

    /**
     * Retrieves the object managed by this scope for the given target. This method is responsible
     * for handling the creation, retrieval, and initialization of the object as per the scope's
     * defined lifecycle.
     *
     * @param target the target object into which the managed object will be injected
     * @return the object managed by this scope
     */
    Object get(Object target);

    /**
     * Associates a value with the given target object within this scope. This method is responsible
     * for setting the value on the target object, potentially managing the lifecycle of the value
     * according to the scope's rules.
     *
     * @param target the target object into which the value will be set
     * @param value the value to be set on the target object
     */
    void set(Object target, Object value);
}

